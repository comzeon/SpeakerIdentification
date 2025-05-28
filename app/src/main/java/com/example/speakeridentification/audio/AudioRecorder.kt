package com.example.speakeridentification.audio

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

interface AudioRecorder {
    fun startRecording()
    fun stopRecording()
    fun release()

    fun isRecording(): Boolean
    fun getLatestAudioData(): ShortArray? // 返回最新一段录音
    fun setOnSpeechFrameListener(listener: OnSpeechFrameListener?)
}

interface OnSpeechFrameListener {
    fun onSpeechFrame(frame: ShortArray)
}

class AudioRecorderImpl(
    private val context: Context,
    private val sampleRate: Int = 16000,
    private val bufferSizeInSeconds: Int = 1,
    private val vadProcessor: VadProcessor? = null
) : AudioRecorder {

    private val isRecording = AtomicBoolean(false)
    private val minBufferSize = AudioRecord.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    )
    private val bufferSize = bufferSizeInSeconds * sampleRate

    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null
    private var latestAudioData: ShortArray? = null

    private var speechListener: OnSpeechFrameListener? = null

    private var outputFile: File? = null
    private var fileOutputStream: FileOutputStream? = null

    override fun startRecording() {
        if (isRecording.get()) return

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBufferSize
            )
        } catch (e: SecurityException) {
            Log.e("AudioRecorder", "录音权限被拒绝", e)
            return
        }

        // 如果没有启用 VAD，就准备写文件
        if (vadProcessor == null) {
            outputFile = createOutputFile()
            fileOutputStream = FileOutputStream(outputFile)
        }

        audioRecord?.startRecording()
        isRecording.set(true)

        recordingThread = Thread {
            val buffer = ShortArray(bufferSize)
            val byteBuffer = ByteBuffer.allocate(buffer.size * 2).order(ByteOrder.LITTLE_ENDIAN)

            while (isRecording.get()) {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (read > 0) {
                    latestAudioData = buffer.copyOf(read)

                    if (vadProcessor != null) {
                        if (vadProcessor.isSpeech(latestAudioData!!)) {
                            Log.d("AudioRecorder", "成功记录")
                            speechListener?.onSpeechFrame(latestAudioData!!)
                        }   else{
                            Log.d("AudioRecorder", "VAD判定为静音")
                        }
                    } else {
                        // 保存为 PCM 文件
                        byteBuffer.clear()
                        for (i in 0 until read) {
                            byteBuffer.putShort(buffer[i])
                        }
                        fileOutputStream?.write(byteBuffer.array(), 0, read * 2)
                    }
                }
            }
        }
        recordingThread?.start()
        Log.d("AudioRecorder", "Recording started")
    }

    override fun stopRecording() {
        if (!isRecording.get()) return

        isRecording.set(false)
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null

        recordingThread?.join()
        recordingThread = null

        fileOutputStream?.close()
        fileOutputStream = null

        Log.d("AudioRecorder", "Recording stopped")
    }

    override fun release() {
        stopRecording()
    }

    override fun isRecording(): Boolean {
        return isRecording.get()
    }

    override fun getLatestAudioData(): ShortArray? {
        return latestAudioData
    }

    override fun setOnSpeechFrameListener(listener: OnSpeechFrameListener?) {
        this.speechListener = listener
    }

    fun getOutputFile(): File? {
        return outputFile
    }

    private fun createOutputFile(): File {
        val dir = File(context.filesDir, "recordings")
        if (!dir.exists()) dir.mkdirs()
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return File(dir, "rec_$timestamp.pcm")
    }
}
//class AudioRecorderImpl(
//    private val sampleRate: Int = 16000,
//    private val bufferSizeInSeconds: Int = 1, // 1秒缓冲
//    private val vadProcessor: VadProcessor
//) : AudioRecorder {
//
//    private val isRecording = AtomicBoolean(false)
//    private val minBufferSize = AudioRecord.getMinBufferSize(
//        sampleRate,
//        AudioFormat.CHANNEL_IN_MONO,
//        AudioFormat.ENCODING_PCM_16BIT
//    )
//    private val bufferSize = bufferSizeInSeconds * sampleRate
//
//    private var audioRecord: AudioRecord? = null
//    private var recordingThread: Thread? = null
//    private var latestAudioData: ShortArray? = null
//
//    private var speechListener: OnSpeechFrameListener? = null
//
//    override fun startRecording() {
//        if (isRecording.get()) return
//
//        try {
//            audioRecord = AudioRecord(
//                MediaRecorder.AudioSource.MIC,
//                sampleRate,
//                AudioFormat.CHANNEL_IN_MONO,
//                AudioFormat.ENCODING_PCM_16BIT,
//                minBufferSize
//            )
//        } catch (e: SecurityException) {
//            Log.e("AudioRecorder", "录音权限被拒绝", e)
//            return
//        }
//
//        audioRecord?.startRecording()
//        isRecording.set(true)
//
//        recordingThread = Thread {
//            val buffer = ShortArray(bufferSize)
//            while (isRecording.get()) {
//                val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
//                if (read > 0) {
//                    latestAudioData = buffer.copyOf(read)
//
//                    if (vadProcessor.isSpeech(latestAudioData!!)){
//                        speechListener?.onSpeechFrame(latestAudioData!!)
//                    }
//                }
//            }
//        }
//        recordingThread?.start()
//        Log.d("AudioRecorder", "Recording started")
//    }
//
//    override fun stopRecording() {
//        if (!isRecording.get()) return
//
//        isRecording.set(false)
//        audioRecord?.stop()
//        audioRecord?.release()
//        audioRecord = null
//        recordingThread?.join()
//        recordingThread = null
//        Log.d("AudioRecorder", "Recording stopped")
//    }
//
//    override fun release() {
//        stopRecording()
//    }
//
//    override fun isRecording(): Boolean {
//        return isRecording.get()
//    }
//
//    override fun getLatestAudioData(): ShortArray? {
//        return latestAudioData
//    }
//
//    override fun setOnSpeechFrameListener(listener: OnSpeechFrameListener?) {
//        this.speechListener = listener
//    }
//}