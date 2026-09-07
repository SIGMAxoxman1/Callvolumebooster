package com.sigmaapps.callvolumebooster

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class CrashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crash)
        val text = intent.getStringExtra("stack_trace") ?: "No details captured."
        findViewById<android.widget.TextView>(R.id.crashText).text = text
    }
}
