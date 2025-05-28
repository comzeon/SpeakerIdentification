package com.example.speakeridentification.audio

import android.util.Log
import kotlin.math.abs

interface VadProcessor {
    /**
     * 传入原始 PCM 音频帧，判断是否包含人声。
     * @param audioData PCM 16-bit 单声道音频（小端）
     * @return true 表示检测到语音，false 表示静音或背景噪声
     */
    fun isSpeech(audioData: ShortArray): Boolean
}

class EnergyBasedVadProcessor(
    private val energyThreshold: Int = 50,  // 可调节门限
    private val minSpeechFrames: Int = 1      // 最小连续语音帧
) : VadProcessor {

    private var speechFrameCount = 0

    override fun isSpeech(audioData: ShortArray): Boolean {
        val avgEnergy = audioData.map { abs(it.toInt()) }.average()
        Log.d("VAD", "energy:${avgEnergy}")

        return if (avgEnergy > energyThreshold) {
            speechFrameCount++
            if (speechFrameCount >= minSpeechFrames) {
                true
            } else {
                false
            }
        } else {
            speechFrameCount = 0
            false
        }
    }
}
