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
        // requires the app to also be the default phone/call-screening app,
        // or to read it via CallLog shortly after the call starts. This
        // extra works on many devices/OEMs but is not guaranteed on all
        // Android 10+ builds — flagged here as a known follow-up.
        val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER) ?: return

        val prefs = context.getSharedPreferences("vip_prefs", Context.MODE_PRIVATE)
        val vipNumber = prefs.getString("vip_number", null) ?: return

        val normalizedIncoming = incomingNumber.filter { it.isDigit() }.takeLast(10)
        if (normalizedIncoming.isNotEmpty() && normalizedIncoming == vipNumber) {
            boostRingVolume(context)
        }
    }

    private fun boostRingVolume(context: Context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)
        audioManager.setStreamVolume(AudioManager.STREAM_RING, maxVolume, 0)
    }
}
