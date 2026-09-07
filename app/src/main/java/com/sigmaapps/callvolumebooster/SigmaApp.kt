package com.sigmaapps.callvolumebooster

import android.app.Application
import android.content.Intent
import android.os.Process
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Catches any uncaught crash anywhere in the app and shows the real error
 * on screen instead of the generic "app has stopped" dialog, so it can be
 * copied and shared for debugging.
 */
class SigmaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))

                val intent = Intent(this, CrashActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                intent.putExtra("stack_trace", sw.toString())
                startActivity(intent)
            } catch (e: Exception) {
                // If even the crash screen fails, fall back to the system default.
            }
            Process.killProcess(Process.myPid())
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
