package com.sigmaapps.callvolumebooster

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

/**
 * A small foreground service whose only job is to keep the app process alive
 * long enough for PhoneStateReceiver (registered at runtime below) to catch
 * incoming-call broadcasts. Android 8+ blocks most implicit broadcasts from
 * reaching manifest-declared receivers, so runtime registration inside a
 * foreground service is the reliable approach across old and new versions.
 */
class CallWatchService : Service() {

    private val receiver = PhoneStateReceiver()
    private val channelId = "call_watch_channel"

    override fun onCreate() {
        super.onCreate()
        registerReceiver(receiver, IntentFilter("android.intent.action.PHONE_STATE"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(1, buildNotification())
        return START_STICKY
    }

    private fun buildNotification(): android.app.Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Call watching",
                NotificationManager.IMPORTANCE_MIN
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Call Volume Booster is active")
            .setContentText("Watching for calls from your VIP contact.")
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode_off)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
