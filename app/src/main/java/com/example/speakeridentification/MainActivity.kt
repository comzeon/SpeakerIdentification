package com.example.speakeridentification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.speakeridentification.audio.AudioPreprocessorImpl
import com.example.speakeridentification.model.ModelRunner

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val modelButton = findViewById<Button>(R.id.buttonTestModel)
        val melButton = findViewById<Button>(R.id.buttonTestMel)
        val notificationButton = findViewById<Button>(R.id.buttonNotification)

        // 初始化模型
        ModelRunner.initialize(this)
        val preprocessor = AudioPreprocessorImpl()

        modelButton.setOnClickListener {
            // 构造 dummy 输入
            val input = FloatArray(1 * 400 * 80) { 0.5f }
            val shape = longArrayOf(1, 400, 80)

            // 推理
            val output = ModelRunner.infer(input, shape)

            // 打印结果前10维
            Log.d("ModelRunner", "Embedding size = ${output.size}")
            Log.d("ModelRunner", "Embedding (first 10): ${output.take(10).joinToString()}")

            Toast.makeText(this, "推理完成，维度=${output.size}", Toast.LENGTH_SHORT).show()
        }
        melButton.setOnClickListener {    // 1. 测试 STFT 输出维度
            // 1. 正常音频输入：测试维度与是否有 NaN
            val sineWave = generateSineWaveShort(16000, 1.0f)
            val features1 = preprocessor.extractFbankFeatures(sineWave)
            if (features1.isNotEmpty() && !features1.any { it.isNaN() }) {
                Log.d("TestFbank-SineWave", "Passed: output length = ${features1.size}")
            } else {
                Log.e("TestFbank-SineWave", "Failed: NaN or empty")
            }

            // 2. 静音输入：测试是否崩溃、是否输出有效特征
            val silent = ShortArray(16000) // 全 0
            val features2 = preprocessor.extractFbankFeatures(silent)
            if (features2.isNotEmpty() && !features2.any { it.isNaN() }) {
                Log.d("TestFbank-Silent", "Passed: output length = ${features2.size}")
            } else {
                Log.e("TestFbank-Silent", "Failed: NaN or empty")
            }

            // 3. 随机输入：测试归一化后最大值是否接近 1
            val random = ShortArray(16000) { ((Math.random() * Short.MAX_VALUE * 2) - Short.MAX_VALUE).toInt().toShort() }
            val features3 = preprocessor.extractFbankFeatures(random)
            val maxAbs = features3.maxOfOrNull { kotlin.math.abs(it) } ?: 0f
            if (kotlin.math.abs(maxAbs - 1.0f) < 1e-2) {
                Log.d("TestFbank-Normalization", "Passed: maxAbs = $maxAbs")
            } else {
                Log.w("TestFbank-Normalization", "Check normalization: maxAbs = $maxAbs")
            }
        }
        notificationButton.setOnClickListener {
            Log.d("TEST", "Notification Entered!")

            val channelId = "unlock_success"
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        100
                    )
                    Log.w("TEST", "未授权 POST_NOTIFICATIONS，已请求权限")
                    return@setOnClickListener
                }
            }

            // 创建通知渠道
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

            val unlockIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            val pendingIntent = PendingIntent.getActivity(
                this, 0, unlockIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val fullScreenIntent = Intent(this, GroupListActivity::class.java)
            val fullScreenPendingIntent = PendingIntent.getActivity(this, 0,
                fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT)

            val notification = NotificationCompat.Builder(this, channelId)
                .setSmallIcon(androidx.core.R.drawable.notification_icon_background)
                .setContentTitle("欢迎回来！用户")
                .setContentText("声纹识别成功，您可以开始使用手机")
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .build()

            manager.notify(1001, notification)
            Log.d("TEST", "通知已发送 manager.notify")
        }
    }

    fun generateSineWaveShort(sampleRate: Int, durationSec: Float): ShortArray {
        val size = (sampleRate * durationSec).toInt()
        val freq = 440.0 // A4
        return ShortArray(size) { i ->
            val sample = kotlin.math.sin(2 * Math.PI * freq * i / sampleRate).toFloat()
            (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
    }

    fun containsNaN(data: Array<FloatArray>): Boolean {
        return data.any { frame -> frame.any { it.isNaN() } }
    }
}