package com.example.aurduinobluetooth

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.bluetoothterminal.MainActivity
import java.util.logging.Handler

class SplashScreen : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        // Duration of splash screen (in milliseconds)
        val SPLASH_TIME_OUT: Long = 3000 // 3 seconds

        // Handler to delay the transition to the next activity
        android.os.Handler().postDelayed({
            // Start MainActivity after the delay
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish() // Close SplashActivity so the user can't go back to it
        }, SPLASH_TIME_OUT)
    }
}