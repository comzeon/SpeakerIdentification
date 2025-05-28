package com.example.speakeridentification.unlock

import android.content.Context
import android.util.Log
import com.example.speakeridentification.model.ModelRunner
import com.example.speakeridentification.audio.AudioPreprocessor
import com.example.speakeridentification.database.RecordingDao
import com.example.speakeridentification.database.UserDao
import com.example.speakeridentification.model.FeatureComparer
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UnlockManager(
    private val context: Context,
    private val modelRunner: ModelRunner,
    private val featureComparer: FeatureComparer,
    private val audioPreprocessor: AudioPreprocessor,
    private val recordingDao: RecordingDao,
    private val userDao: UserDao
) {
    interface UnlockCallback {
        fun onUnlockSuccess(reason: String, name: String, score: Float)
        fun onUnlockFailure(reason: String)
    }

    private var registeredFeatures: Map<String, FloatArray> = emptyMap()

    suspend fun loadRegisteredFeaturesFromDB() {
        val gson = Gson()
        val recordings = recordingDao.getAll()
        val userMap = mutableMapOf<String, FloatArray>()
        for (recording in recordings) {
            try {
                val embedding = gson.fromJson(recording.embedding, FloatArray::class.java)
                userMap[recording.userId.toString()] = embedding
            } catch (e: Exception) {
                Log.e("UnlockManager", "解析特征失败: ${recording.name}", e)
            }
        }
        registeredFeatures = userMap
    }

    fun processAudioForUnlock(audioData: ShortArray, callback: UnlockCallback) {
        Log.d("PROCESSING", ".......")
        val fbankFeatures = audioPreprocessor.extractFbankFeatures(audioData)
        val embedding = modelRunner.infer(fbankFeatures, longArrayOf(1,
            audioPreprocessor.getFixedLength().toLong(), audioPreprocessor.getNMelBins().toLong()))

        if (embedding == null) {
            callback.onUnlockFailure("推理失败，无法提取特征")
            return
        }

        // 遍历已注册的特征
        var bestScore = -1f
        var bestUser: String? = null

        for ((userId, userEmbedding) in registeredFeatures) {
            val score = featureComparer.calculateSimilarity(embedding, userEmbedding)
            if (score > bestScore) {
                bestScore = score
                bestUser = userId
            }
        }

        val thresholdLow = 0.6f   // 低阈值，低于此认为失败
        val thresholdHigh = 0.995f  // 高阈值，过高防止回放攻击

        CoroutineScope(Dispatchers.IO).launch {
            val bestUserName = try {
                bestUser?.toLongOrNull()?.let { userDao.getUserById(it)?.name } ?: "未知用户"
            } catch (e: Exception) {
                Log.e("UnlockManager", "获取用户名失败: $bestUser", e)
                "未知用户"
            }

            withContext(Dispatchers.Main) {
                if (bestScore in thresholdLow..thresholdHigh) {
                    callback.onUnlockSuccess("识别成功", bestUserName, bestScore)
                } else {
                    callback.onUnlockFailure("识别失败，最佳得分：$bestScore，用户：$bestUserName")
                }
            }
        }
    }

    //    fun registerUserFeatures(userFeatures: Map<String, FloatArray>) {
//        // 外部传入，管理好用户保存的特征，比如 {"user1" to 特征向量}
//        registeredFeatures = userFeatures
//    }
}