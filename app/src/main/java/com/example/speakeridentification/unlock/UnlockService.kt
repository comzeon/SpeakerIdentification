package com.example.speakeridentification.unlock

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.speakeridentification.R
import com.example.speakeridentification.audio.AudioPreprocessorImpl
import com.example.speakeridentification.audio.AudioRecorderImpl
import com.example.speakeridentification.audio.EnergyBasedVadProcessor
import com.example.speakeridentification.audio.OnSpeechFrameListener
import com.example.speakeridentification.model.FeatureComparer
import com.example.speakeridentification.model.ModelRunner
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import com.example.speakeridentification.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UnlockService : Service() {

    private lateinit var audioRecorder: AudioRecorderImpl
    private lateinit var unlockManager: UnlockManager

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()

        // 初始化模型
//        ModelRunner.initialize(this)

        val database = AppDatabase.getInstance(applicationContext)
        val recordingDao = database.recordingDao()
        val userDao = database.userDao()
        // 初始化解锁管理器
        unlockManager = UnlockManager(
            context = this,
            modelRunner = ModelRunner,
            featureComparer = FeatureComparer,
            audioPreprocessor = AudioPreprocessorImpl(),
            recordingDao = recordingDao,
            userDao = userDao
        )

        val channel = NotificationChannel(
            "voice_unlock", "声纹解锁通知",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
        // 最小化通知影响（Android 12+要求必须有通知）
        val notification = NotificationCompat.Builder(this, "voice_unlock")
            .setContentTitle("声纹解锁中")
            .setContentText("正在识别您的声音...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW) // 降低优先级
            .setOngoing(false)
            .build()
        startForeground(1, notification)

        // 设置超时退出：万一没人说话 10 秒后退出
        Handler(Looper.getMainLooper()).postDelayed({
            this@UnlockService.stopSelf()
        }, 10_000)

        val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        serviceScope.launch {
            unlockManager.loadRegisteredFeaturesFromDB()

            withContext(Dispatchers.Main) {
                val vadProcessor = EnergyBasedVadProcessor()
                audioRecorder = AudioRecorderImpl(applicationContext, vadProcessor = vadProcessor)
                audioRecorder.setOnSpeechFrameListener(object : OnSpeechFrameListener {
                    override fun onSpeechFrame(frame: ShortArray) {
                        Log.d("OnSpeechFrame", "唤醒unlockManager")
                        unlockManager.processAudioForUnlock(frame, object : UnlockManager.UnlockCallback {
                            override fun onUnlockSuccess(reason: String, name: String, score: Float) {
                                Log.d("SUCCESS", "Welcome!")

                                Handler(Looper.getMainLooper()).post {
                                    val channelId = "unlock_success"
                                    val manager = getSystemService(NotificationManager::class.java)

                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        val channel = NotificationChannel(
                                            channelId,
                                            "解锁成功通知",
                                            NotificationManager.IMPORTANCE_HIGH
                                        ).apply {
                                            description = "用于锁屏状态下的解锁成功提示"
                                            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                                        }
                                        manager.createNotificationChannel(channel)
                                    }

                                    val intent = Intent(Intent.ACTION_MAIN).apply {
                                        addCategory(Intent.CATEGORY_HOME)
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }

                                    val pendingIntent = PendingIntent.getActivity(
                                        this@UnlockService, 0, intent,
                                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                                    )

                                    val notification = NotificationCompat.Builder(this@UnlockService, channelId)
                                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                                        .setContentTitle("欢迎回来！用户：${name}，您的得分为：${score}分")
                                        .setContentText("声纹识别成功，您可以开始使用手机")
                                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                                        .setAutoCancel(true)
                                        .setContentIntent(pendingIntent)
                                        .build()

                                    manager.notify(1001, notification)
                                }
                                Log.w("Unlock", "Unlock success!: $reason")
//                                val unlockIntent = Intent(Intent.ACTION_MAIN).apply {
//                                    addCategory(Intent.CATEGORY_HOME)
//                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
//                                }
//                                startActivity(unlockIntent)
                                this@UnlockService.stopSelf()
                            }

                            override fun onUnlockFailure(reason: String) {
                                Log.w("Unlock", "Unlock failed: $reason")
                            }
                        })
                    }
                })
                audioRecorder.startRecording()
            }
        }
    }

    override fun onDestroy() {
        audioRecorder.stopRecording()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}