package com.example.voiceapp3

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.i("MainActivity", "Launched as APP, starting service")
        val serviceIntent = Intent(applicationContext, VoiceAssistantService::class.java)
        applicationContext.startForegroundService(serviceIntent)

    }
}