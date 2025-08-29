package com.baidu.bridge.server

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.example.voiceapp3.VoiceAssistantService

class AIDLService : Service() {
    companion object {
        private const val TAG = "AIDLService"
        private const val NOTIFICATION_CHANNEL_ID = "AIDLServiceChannel"
        private const val NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val serviceIntent = Intent(applicationContext, VoiceAssistantService::class.java)
        startForegroundService(serviceIntent)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "AIDL Service Channel",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun startForeground() {
        val notification =
            Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle("AIDL Service")
                .setContentText("Service is running")
                .build()

        startForeground(NOTIFICATION_ID, notification)
    }
}