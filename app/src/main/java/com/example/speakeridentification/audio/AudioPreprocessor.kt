package com.example.speakeridentification.audio

import kotlin.math.*
import org.jtransforms.fft.FloatFFT_1D

interface AudioPreprocessor {
    fun extractFbankFeatures(audioData: ShortArray): FloatArray
    fun getNMelBins(): Int
    fun getFixedLength(): Int
}

class AudioPreprocessorImpl(
    private val sampleRate: Int = 16000,
    private val nMelBins: Int = 80,
    private val fixedLength: Int = 400,
    private val frameLengthMs: Int = 25,
    private val frameShiftMs: Int = 10
) : AudioPreprocessor {

    private val frameLength = sampleRate * frameLengthMs / 1000
    private val frameShift = sampleRate * frameShiftMs / 1000

    override fun extractFbankFeatures(audioData: ShortArray): FloatArray {
        // 1. 归一化 short -> float [-1, 1]
        val normalizedAudio = audioData.map { it / 32768.0f }.toFloatArray()
        val mean = normalizedAudio.average().toFloat()
        val zeroMeanAudio = normalizedAudio.map { it - mean }.toFloatArray()

        // 2. 分帧
        val frames = framing(zeroMeanAudio)

        // 3. 加窗（比如 Hamming窗）
        val window = hammingWindow(frameLength)
        for (i in frames.indices) {
            for (j in frames[i].indices) {
                frames[i][j] *= window[j]
            }
        }

        // 4. 对每帧做FFT -> 取幅度谱
        val powerSpectrogram = frames.map { frame ->
            fftPower(frame)
        }

        // 5. 将幅度谱投影到 Mel 滤波器组上
        val melSpectrogram = melFilterBank(powerSpectrogram)

        // 6. 取 log
        val logMelSpectrogram = melSpectrogram.map { frame ->
            FloatArray(nMelBins) { i -> log10(frame[i] + 1e-6f) }
        }

        // 7. 补零或裁剪为 400 帧
        val padded = Array(fixedLength) { FloatArray(nMelBins) { 0f } }

        for (i in 0 until min(fixedLength, logMelSpectrogram.size)) {
            padded[i] = logMelSpectrogram[i]
        }

        // 8. 拉平成一维数组作为输出
        return padded.flatMap { it.asList() }.toFloatArray()
    }

    override fun getNMelBins(): Int {return nMelBins}
    override fun getFixedLength(): Int {return fixedLength}

    private fun framing(waveform: FloatArray): List<FloatArray> {
        val frames = mutableListOf<FloatArray>()
        var pos = 0
        while (pos + frameLength <= waveform.size) {
            frames.add(waveform.sliceArray(pos until (pos + frameLength)))
            pos += frameShift
        }
        return frames
    }

    private fun hammingWindow(length: Int): FloatArray {
        return FloatArray(length) { i ->
            (0.54 - 0.46 * cos(2 * Math.PI * i / (length - 1))).toFloat()
        }
    }

    private fun fftPower(frame: FloatArray): FloatArray {
        val fftSize = nextPowerOfTwo(frame.size)
        val fft = FloatFFT_1D(fftSize.toLong())

        val fftData = FloatArray(fftSize * 2)
        for (i in frame.indices) {
            fftData[2 * i] = frame[i]
            fftData[2 * i + 1] = 0f
        }

        fft.complexForward(fftData)

        val power = FloatArray(fftSize / 2 + 1)
        for (i in 0 until power.size) {
            val re = fftData[2 * i]
            val im = fftData[2 * i + 1]
            power[i] = re * re + im * im
        }

        return power
    }

    private fun melFilterBank(spectrogram: List<FloatArray>): List<FloatArray> {
        val fftSize = (spectrogram[0].size - 1) * 2
        val melFilterBank = createMelFilterBank(fftSize, sampleRate, nMelBins)

        return spectrogram.map { frame ->
            FloatArray(nMelBins) { melBin ->
                var energy = 0f
                for (k in frame.indices) {
                    energy += frame[k] * melFilterBank[melBin][k]
                }
                energy
            }
        }
    }

    private fun createMelFilterBank(fftSize: Int, sampleRate: Int, nMelBins: Int): Array<FloatArray> {
        val lowFreq = 0
        val highFreq = sampleRate / 2

        fun hzToMel(hz: Double): Double = 2595 * log10(1 + hz / 700)
        fun melToHz(mel: Double): Double = 700 * (10.0.pow(mel / 2595) - 1)

        val melLow = hzToMel(lowFreq.toDouble())
        val melHigh = hzToMel(highFreq.toDouble())
        val melPoints = DoubleArray(nMelBins + 2) { i ->
            melLow + (melHigh - melLow) * i / (nMelBins + 1)
        }

        val hzPoints = melPoints.map { melToHz(it) }
        val binPoints = hzPoints.map { floor((fftSize + 1) * it / sampleRate).toInt() }

        val filterBank = Array(nMelBins) { FloatArray(fftSize / 2 + 1) }

        for (i in 0 until nMelBins) {
            val left = binPoints[i]
            val center = binPoints[i + 1]
            val right = binPoints[i + 2]

            for (j in left until center) {
                if (j in filterBank[i].indices) {
                    filterBank[i][j] = ((j - left).toFloat()) / (center - left)
                }
            }
            for (j in center until right) {
                if (j in filterBank[i].indices) {
                    filterBank[i][j] = ((right - j).toFloat()) / (right - center)
                }
            }
        }

        return filterBank
    }

    private fun nextPowerOfTwo(n: Int): Int {
        var v = n
        v--
        v = v or (v shr 1)
        v = v or (v shr 2)
        v = v or (v shr 4)
        v = v or (v shr 8)
        v = v or (v shr 16)
        v++
        return v
    }
}
