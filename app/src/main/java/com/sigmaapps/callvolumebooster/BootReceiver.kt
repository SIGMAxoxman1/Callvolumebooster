package com.sigmaapps.callvolumebooster

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.READ_PHONE_STATE
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            if (hasPermission && VipStore.isWatcherEnabled(context)) {
                val serviceIntent = Intent(context, CallWatchService::class.java)
                ContextCompat.startForegroundService(context, serviceIntent)
            }
        }
    }
}
