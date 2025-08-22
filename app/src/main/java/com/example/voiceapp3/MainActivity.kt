package com.example.voiceapp3

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView
import androidx.core.view.isVisible
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class MainActivity : AppCompatActivity() {
    private lateinit var localBroadcastManager: LocalBroadcastManager
    private lateinit var loadingLayout: View
    private lateinit var mainContent: View
    private lateinit var micAnimation: LottieAnimationView
    private lateinit var serviceReadyReceiver: BroadcastReceiver
    private val handler = Handler(Looper.getMainLooper()) // Add this line

    companion object {
        const val ACTION_SERVICE_READY = "com.example.voiceapp3.SERVICE_READY"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        localBroadcastManager = LocalBroadcastManager.getInstance(this)

        // Initialize views
        loadingLayout = findViewById(R.id.loading_layout)
        mainContent = findViewById(R.id.main_content)
        micAnimation = findViewById(R.id.mic_animation)

        // Show loading screen immediately
        if (VoiceAssistantService.isServiceReady()) {
            showContent()
        } else {
            showLoading()
        }

        // Setup service ready receiver
        serviceReadyReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == ACTION_SERVICE_READY) {
                    runOnUiThread {
                        showContent()
                    }
                }
            }
        }

        // Register the receiver FIRST before starting service
        val filter = IntentFilter(ACTION_SERVICE_READY)
        localBroadcastManager.registerReceiver(serviceReadyReceiver, filter)

        // Delay service start until after UI is rendered
        handler.postDelayed({
            Log.i("MainActivity", "Starting service after UI render")
            val serviceIntent = Intent(applicationContext, VoiceAssistantService::class.java)
            applicationContext.startForegroundService(serviceIntent)
        }, 100) // Short delay to ensure UI is rendered first
    }

    private fun showLoading() {
        loadingLayout.visibility = View.VISIBLE
        mainContent.visibility = View.GONE
    }

    private fun showContent() {
        loadingLayout.visibility = View.GONE
        mainContent.visibility = View.VISIBLE
        micAnimation.playAnimation()
        Log.i("MainActivity", "Main content shown and animation started")
    }

    override fun onResume() {
        super.onResume()
        if (mainContent.isVisible) {
            micAnimation.resumeAnimation()
        }
    }

    override fun onPause() {
        super.onPause()
        micAnimation.pauseAnimation()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            localBroadcastManager.unregisterReceiver(serviceReadyReceiver)
        } catch (e: IllegalArgumentException) {
            // Receiver was not registered, ignore
        }
    }
}