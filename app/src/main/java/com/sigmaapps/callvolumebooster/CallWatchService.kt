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
 * Foreground service that keeps the app process alive so
 * PhoneStateReceiver (registered at runtime below) can catch incoming-call
 * broadcasts. Android 8+ blocks most implicit broadcasts from reaching
 * manifest-declared receivers, so runtime registration inside a foreground
 * service is the reliable approach across old and new Android versions.
 * Started on first launch, and again on boot by BootReceiver.
 */
class CallWatchService : Service() {

    private val receiver = PhoneStateReceiver()
    private val channelId = "call_watch_channel"

    override fun onCreate() {
        super.onCreate()
        val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            this, android.Manifest.permission.READ_PHONE_STATE
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            registerReceiver(receiver, IntentFilter("android.intent.action.PHONE_STATE"))
        } else {
            stopSelf()
        }
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
            .setContentText("Watching for calls from your VIP contacts.")
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode_off)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(receiver)
        } catch (e: IllegalArgumentException) {
            // Receiver was never registered — safe to ignore.
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
