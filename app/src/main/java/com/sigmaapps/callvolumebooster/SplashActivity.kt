package com.sigmaapps.callvolumebooster

import android.content.Intent
import android.os.Bundle
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val logo = findViewById<android.widget.TextView>(R.id.splashLogo)
        val label = findViewById<android.widget.TextView>(R.id.splashLabel)

        logo.scaleX = 0.6f
        logo.scaleY = 0.6f

        logo.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(500)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                label.animate().alpha(1f).setDuration(350).start()
            }
            .start()

        android.os.Handler(mainLooper).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, 1300)
    }
}
