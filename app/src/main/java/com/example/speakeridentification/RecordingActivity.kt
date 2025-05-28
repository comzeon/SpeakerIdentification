package com.example.speakeridentification

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.speakeridentification.database.AppDatabase
import com.example.speakeridentification.database.Recording
import com.example.speakeridentification.database.RecordingAdapter
import com.example.speakeridentification.database.RecordingDao
import com.example.speakeridentification.audio.AudioRecorderImpl
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.widget.EditText
import android.widget.Toast
import android.Manifest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.lifecycle.lifecycleScope
import com.example.speakeridentification.audio.AudioPreprocessor
import com.example.speakeridentification.audio.AudioPreprocessorImpl
import com.example.speakeridentification.audio.VadProcessor
import com.example.speakeridentification.database.UserGroup
import com.example.speakeridentification.model.ModelRunner
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException

class RecordingActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var recordingDao: RecordingDao
    private lateinit var adapter: RecordingAdapter
    private lateinit var recorder: AudioRecorderImpl
    private val audioPreprocessor: AudioPreprocessor = AudioPreprocessorImpl()
    private val recordings = mutableListOf<Recording?>()
    private var userId: Long = -1
    private var userName: String = ""
    private val REQUEST_RECORD_AUDIO_PERMISSION = 200

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.recording_list)
//        recorder = AudioRecorderImpl(applicationContext, vadProcessor = object : VadProcessor {
//            override fun isSpeech(audioData: ShortArray): Boolean = true
//        })
        recorder = AudioRecorderImpl(applicationContext, vadProcessor = null)


        userId = intent.getLongExtra("user_id", -1)
        userName = intent.getStringExtra("user_name") ?: ""
        if (userId == -1L) {
            finish()
            return
        }

        title = "用户：$userName 的录音"

        db = AppDatabase.getInstance(this)
        recordingDao = db.recordingDao()

        val recyclerView = findViewById<RecyclerView>(R.id.rvRecordingList)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = RecordingAdapter(recordings,
            onRecordingClick = { recording ->
                if (recording == null) showAddRecordingDialog()
                else {
                    Toast.makeText(this, "点击了录音：${recording.name}", Toast.LENGTH_SHORT).show()
                    // 后续播放逻辑放这里
//                playRecording(recording)
                    playPCM(recording)
                }
            },
            onRecordingRename = {recording -> showRenameRecordingDialog(recording)},
            onRecordingDelete = {recording -> confirmDeleteRecording(recording)})

        recyclerView.adapter = adapter

        loadRecordings()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun loadRecordings() {
        lifecycleScope.launch {
            val list = recordingDao.getRecordingsForUser(userId)
            recordings.clear()
            recordings.addAll(list)
            recordings.add(null)
            adapter.notifyDataSetChanged()
        }
    }

    private fun showAddRecordingDialog() {
        val context = this
        val editText = EditText(this).apply {
            hint = "请输入录音名称"
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle("新录音")
            .setView(editText)
            .setCancelable(false)
            .setNegativeButton("取消") { _, _ ->
                if (recorder.isRecording()) recorder.stopRecording()
            }
            .create()

        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "开始录音") { _, _ -> } // 我们在 show 后设置点击逻辑
        dialog.show()

        val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        var isRecording = false

        positiveButton.setOnClickListener {
            val name = editText.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(context, "名称不能为空", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isRecording) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.RECORD_AUDIO),
                        REQUEST_RECORD_AUDIO_PERMISSION
                    )
                    return@setOnClickListener
                }
                // 开始录音
                recorder.startRecording()
                positiveButton.text = "停止并保存"
                editText.isEnabled = false
                isRecording = true
            } else {
                // 停止录音并保存
                recorder.stopRecording()

                val audioData: ShortArray? = recorder.getLatestAudioData()
                if (audioData == null) {
                    Toast.makeText(context, "录音失败，无法获取音频数据", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val fbankFeatures = audioPreprocessor.extractFbankFeatures(audioData)
                Log.d("DEBUG", "fbankFeatures size = ${fbankFeatures.size}") // 应该是帧数，比如 400
                val embeddingArray = ModelRunner.infer(fbankFeatures, longArrayOf(1,
                    audioPreprocessor.getFixedLength().toLong(), audioPreprocessor.getNMelBins().toLong()))

                if (embeddingArray == null) {
                    Toast.makeText(context, "推理失败，无法提取特征", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val embeddingJson = Gson().toJson(embeddingArray)

                val file = recorder.getOutputFile()
                if (file != null && file.exists()) {
                    val newRecording = Recording(
                        name = name,
                        filePath = file.absolutePath,
                        embedding = embeddingJson,
                        userId = userId,
                        timestamp = System.currentTimeMillis()
                    )

                    lifecycleScope.launch {
                        recordingDao.insert(newRecording)
                        loadRecordings()
                    }

                    Toast.makeText(context, "录音已保存: ${file.name}", Toast.LENGTH_SHORT).show()
                    Log.d("Recording", "File saved at: ${file.absolutePath}")
                } else {
                    Toast.makeText(context, "保存失败，文件不存在", Toast.LENGTH_SHORT).show()
                }

                dialog.dismiss()
            }
        }
    }

//    private fun playRecording(recording: Recording) {
//        val file = File(recording.filePath)
//
//        if (file.exists()) {
//            val mediaPlayer = MediaPlayer()
//            try {
//                mediaPlayer.setDataSource(file.absolutePath)
//                mediaPlayer.setOnPreparedListener { mp ->
//                    mp?.start()  // 准备完成后开始播放
//                }
//                mediaPlayer.setOnErrorListener { mp, what, extra ->
//                    // 记录错误日志，并返回 true 防止进一步处理
//                    Log.e("MediaPlayer", "播放文件出错: $what, $extra")
//                    true
//                }
//                // 设置播放完成监听器，播放完成后可以做一些其他操作，如释放资源
//                mediaPlayer.setOnCompletionListener {
//                    mediaPlayer.release()  // 释放资源
//                    Toast.makeText(this, "播放完成", Toast.LENGTH_SHORT).show()
//                }
//
//                mediaPlayer.prepareAsync()
//            } catch (e: IOException) {
//                e.printStackTrace()
//                Toast.makeText(this, "播放失败：${e.message}", Toast.LENGTH_SHORT).show()
//            }
//        } else {
//            Toast.makeText(this, "录音文件不存在", Toast.LENGTH_SHORT).show()
//        }
//    }

    private fun playPCM(recording: Recording) {
        val file = File(recording.filePath)

        if (file.exists()) {
            val sampleRate = 16000  // 示例：16kHz
            val channelConfig = AudioFormat.CHANNEL_OUT_MONO  // 单声道
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT  // 16位采样

            val bufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)

            val audioTrack = AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize,
                AudioTrack.MODE_STREAM
            )
            try {
                val inputStream = BufferedInputStream(FileInputStream(file))
                val buffer = ByteArray(bufferSize)

                audioTrack.play()

                var read: Int
                while (inputStream.read(buffer).also { read = it } > 0) {
                    audioTrack.write(buffer, 0, read)
                }

                inputStream.close()
                audioTrack.stop()
                audioTrack.release()
                Toast.makeText(this, "播放完成", Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
                Log.e("PCM", "播放失败: ${e.message}")
            }
        }   else {
            Toast.makeText(this, "录音文件不存在", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showAddRecordingDialog()
            } else {
                Toast.makeText(this, "未授予录音权限", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showRenameRecordingDialog(recording: Recording) {
        val editText = EditText(this).apply {
            setText(recording.name)
        }

        AlertDialog.Builder(this)
            .setTitle("重命名录音")
            .setView(editText)
            .setPositiveButton("确定") { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty()) {
                    lifecycleScope.launch {
                        recordingDao.update(recording.copy(name = newName))
                        loadRecordings()
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun confirmDeleteRecording(recording: Recording) {
        AlertDialog.Builder(this)
            .setTitle("确认删除")
            .setMessage("确定要删除录音 \"${recording.name}\" 吗？此操作不可恢复。")
            .setPositiveButton("删除") { _, _ ->
                lifecycleScope.launch {
                    recordingDao.delete(recording)
                    loadRecordings()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }
}
