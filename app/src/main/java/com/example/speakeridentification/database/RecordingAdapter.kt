package com.example.speakeridentification.database

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.example.speakeridentification.R

class RecordingAdapter(
    private val recordings: List<Recording?>,
    private val onRecordingClick: (Recording?) -> Unit,
    private val onRecordingRename: (Recording) -> Unit,
    private val onRecordingDelete: (Recording) -> Unit
) : RecyclerView.Adapter<RecordingAdapter.RecordingViewHolder>() {

    override fun getItemCount(): Int = recordings.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recording, parent, false)
        return RecordingViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecordingViewHolder, position: Int) {
        val recording = recordings[position]
        holder.bind(recording)
    }

    private fun showRecordingOptionsDialog(context: Context, recording: Recording) {
        AlertDialog.Builder(context)
            .setTitle("录音文件操作")
            .setItems(arrayOf("重命名", "删除")) { _, which ->
                when (which) {
                    0 -> onRecordingRename(recording)
                    1 -> onRecordingDelete(recording)
                }
            }
            .show()
    }

    inner class RecordingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val nameView: TextView = view.findViewById(R.id.tvRecording)

        fun bind(recording: Recording?) {
            nameView.text = recording?.name ?: "🎤 新建录音"
            itemView.setOnClickListener { onRecordingClick(recording) }

            if (recording != null) {
                itemView.setOnLongClickListener {
                    showRecordingOptionsDialog(it.context, recording)
                    true
                }
            } else {
                itemView.setOnLongClickListener(null)
            }
        }
    }
}
