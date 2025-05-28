package com.example.speakeridentification.unlock

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.speakeridentification.R

class ScreenRegisterService : Service() {

    private val screenReceiver = ScreenReceiver()

    override fun onCreate() {
        super.onCreate()

        val filter = IntentFilter(Intent.ACTION_SCREEN_ON)
        applicationContext.registerReceiver(screenReceiver, filter)

        // 创建通知渠道
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "register_channel", "Screen Receiver",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)
                ?.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, "register_channel")
            .setContentTitle("保持监听")
            .setContentText("已注册 ScreenReceiver")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()

        startForeground(1, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        applicationContext.unregisterReceiver(screenReceiver)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}