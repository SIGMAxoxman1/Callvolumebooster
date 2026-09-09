package com.sigmaapps.callvolumebooster

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.telephony.TelephonyManager

class PhoneStateReceiver : BroadcastReceiver() {

    companion object {
        // Remembers the phone's ring state from just before we boosted it,
        // so we can put it back the way it was once the call ends.
        private var previousRingerMode: Int? = null
        private var previousRingVolume: Int? = null
        private var previousInterruptionFilter: Int? = null
        private var didBoost = false
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.getStringExtra(TelephonyManager.EXTRA_STATE)) {
            TelephonyManager.EXTRA_STATE_RINGING -> handleRinging(context, intent)
            TelephonyManager.EXTRA_STATE_IDLE -> restoreIfNeeded(context)
        }
    }

    private fun handleRinging(context: Context, intent: Intent) {
        val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER) ?: return
        val normalized = VipStore.normalize(incomingNumber)
        if (normalized.isEmpty()) return

        val isVip = VipStore.getAll(context).any { it.number == normalized }
        if (isVip) boostRingVolume(context)
    }

    private fun boostRingVolume(context: Context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val notificationManager = context.getSystemService(NotificationManager::class.java)

        // Actually letting the ringtone through requires Do Not Disturb
        // access — the same permission we ask for on first launch. Without
        // it, Android throws a SecurityException on the calls below.
        val canBypassDnd = notificationManager?.isNotificationPolicyAccessGranted == true

        try {
            previousRingerMode = audioManager.ringerMode
            previousRingVolume = audioManager.getStreamVolume(AudioManager.STREAM_RING)
            previousInterruptionFilter = notificationManager?.currentInterruptionFilter

            if (!canBypassDnd) {
                if (audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL) return
            } else {
                // This is the real fix: Do Not Disturb is a system-wide
                // filter that keeps re-silencing the ringer even if we only
                // change the ringer mode or volume. We have to turn the
                // filter itself off for the call to actually make sound.
                notificationManager?.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
            }

            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)
            audioManager.setStreamVolume(AudioManager.STREAM_RING, maxVolume, 0)
            didBoost = true
        } catch (e: SecurityException) {
            // Missing Do Not Disturb access — nothing more we can do until
            // the user grants it from the app.
        }
    }

    private fun restoreIfNeeded(context: Context) {
        if (!didBoost) return
        didBoost = false
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            previousRingVolume?.let { audioManager.setStreamVolume(AudioManager.STREAM_RING, it, 0) }
            previousRingerMode?.let { audioManager.ringerMode = it }
            previousInterruptionFilter?.let { notificationManager?.setInterruptionFilter(it) }
        } catch (e: SecurityException) {
            // Ignore — best effort restore.
        }
    }
}
