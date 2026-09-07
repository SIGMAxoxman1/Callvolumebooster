package com.sigmaapps.callvolumebooster

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.telephony.TelephonyManager

class PhoneStateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        if (state != TelephonyManager.EXTRA_STATE_RINGING) return

        // Note: reading the incoming number reliably on Android 10+ generally
        // requires the app to also be the default phone/call-screening app.
        // This works on many devices but is not guaranteed on every Android
        // 10+ build — flagged as a known follow-up in the README.
        val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER) ?: return
        val normalizedIncoming = VipStore.normalize(incomingNumber)
        if (normalizedIncoming.isEmpty()) return

        val isVip = VipStore.getAll(context).any { it.number == normalizedIncoming }
        if (isVip) {
            boostRingVolume(context)
        }
    }

    private fun boostRingVolume(context: Context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val notificationManager = context.getSystemService(android.app.NotificationManager::class.java)

        // Changing the ring stream while the phone is in Do Not Disturb throws
        // a SecurityException unless the app has notification-policy access.
        val canBypassDnd = notificationManager?.isNotificationPolicyAccessGranted == true
        if (audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL && !canBypassDnd) {
            return
        }

        try {
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)
            audioManager.setStreamVolume(AudioManager.STREAM_RING, maxVolume, 0)
        } catch (e: SecurityException) {
            // Missing Do Not Disturb access — nothing we can do until the
            // user grants it from the app.
        }
    }
}
