package com.example.speakeridentification

import android.app.Application
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.example.speakeridentification.audio.AudioPreprocessorImpl
import com.example.speakeridentification.audio.AudioRecorderImpl
import com.example.speakeridentification.audio.EnergyBasedVadProcessor
import com.example.speakeridentification.audio.OnSpeechFrameListener
import com.example.speakeridentification.model.FeatureComparer
import com.example.speakeridentification.model.ModelRunner
import com.example.speakeridentification.unlock.ScreenReceiver
import com.example.speakeridentification.unlock.ScreenRegisterService
import com.example.speakeridentification.unlock.UnlockManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

class SpeakerApplication : Application() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()

        ModelRunner.initialize(this)

        // 注册屏幕亮起广播监听
        val serviceIntent = Intent(this, ScreenRegisterService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }
}