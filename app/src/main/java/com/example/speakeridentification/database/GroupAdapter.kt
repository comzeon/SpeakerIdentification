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

class GroupAdapter(
    private val groups: List<UserGroup?>,
    private val onGroupClick: (UserGroup?) -> Unit,
    private val onGroupRename: (UserGroup) -> Unit,
    private val onGroupDelete: (UserGroup) -> Unit
) : RecyclerView.Adapter<GroupAdapter.GroupViewHolder>() {

    override fun getItemCount(): Int = groups.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user_group, parent, false)
        return GroupViewHolder(view)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        val group = groups[position]
        holder.bind(group)
    }

    private fun showGroupOptionsDialog(context: Context, group: UserGroup) {
        AlertDialog.Builder(context)
            .setTitle("用户组操作")
            .setItems(arrayOf("重命名", "删除")) { _, which ->
                when (which) {
                    0 -> onGroupRename(group)
                    1 -> onGroupDelete(group)
                }
            }
            .show()
    }

    inner class GroupViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val nameView: TextView = view.findViewById(R.id.tvGroupName)

        fun bind(group: UserGroup?) {
            nameView.text = group?.name?:"➕ 添加新组"
            itemView.setOnClickListener { onGroupClick(group) }

            if (group != null) {
                itemView.setOnLongClickListener {
                    showGroupOptionsDialog(it.context, group)
                    true
                }
            } else {
                itemView.setOnLongClickListener(null)
            }
        }
    }
}