package com.example.speakeridentification.unlock

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat

class ScreenReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent?.action

        if (action == Intent.ACTION_SCREEN_ON) {
            val km = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            val isLocked = km.isKeyguardLocked

            if (isLocked) {
                Log.d("ScreenReceiver", "屏幕亮且处于锁屏状态，准备执行声纹解锁逻辑")
                Handler(Looper.getMainLooper()).postDelayed({
                    Log.d("ScreenReceiver", "Hello, world") // 模拟声纹解锁服务
                }, 1000)
                ContextCompat.startForegroundService(context, Intent(context, UnlockService::class.java))
            } else {
                Log.d("ScreenReceiver", "设备已解锁，跳过声纹解锁逻辑")
            }
        }
    }
    //        if (intent.action == Intent.ACTION_SCREEN_ON) {
//            Log.d("ScreenReceiver", "屏幕亮起，启动录音")
//            val app = context.applicationContext as SpeakerApplication
//            val audioRecorder = app.audioRecorder
//            if (!audioRecorder.isRecording()) {
//                audioRecorder.startRecording()
//            }
//        }

//        when (intent?.action) {
//            Intent.ACTION_BOOT_COMPLETED -> {
//                Log.d("BootAndScreenReceiver", "Boot completed detected")
//            }
//            Intent.ACTION_SCREEN_ON -> {
//                Log.d("BootAndScreenReceiver", "Screen turned on detected")
//                Handler(Looper.getMainLooper()).postDelayed({
//                    Log.d("BootAndScreenReceiver", "Hello, world")
//                }, 1000)
//            }
//        }
}
