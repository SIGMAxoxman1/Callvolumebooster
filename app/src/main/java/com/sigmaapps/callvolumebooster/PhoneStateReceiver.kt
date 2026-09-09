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

        // Actually silencing/unsilencing the ringer (not just its volume level)
        // requires Do Not Disturb access — the same permission we ask for on
        // first launch. Without it, Android throws a SecurityException here.
        val canBypassDnd = notificationManager?.isNotificationPolicyAccessGranted == true

        try {
            previousRingerMode = audioManager.ringerMode
            previousRingVolume = audioManager.getStreamVolume(AudioManager.STREAM_RING)

            if (audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL) {
                if (!canBypassDnd) return
                // This is the actual fix: switch the ringer itself out of
                // silent/vibrate/Do Not Disturb, not just raise the volume.
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
            previousRingVolume?.let { audioManager.setStreamVolume(AudioManager.STREAM_RING, it, 0) }
            previousRingerMode?.let { audioManager.ringerMode = it }
        } catch (e: SecurityException) {
            // Ignore — best effort restore.
        }
    }
}
