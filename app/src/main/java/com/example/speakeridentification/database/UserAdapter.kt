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

class UserAdapter(
    private val users: List<User?>,
    private val onUserClick: (User?) -> Unit,
    private val onUserRename: (User) -> Unit,
    private val onUserDelete: (User) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    override fun getItemCount(): Int = users.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]
        holder.bind(user)
    }

    private fun showGroupDetailsDialog(context: Context, user: User) {
        AlertDialog.Builder(context)
            .setTitle("用户操作")
            .setItems(arrayOf("重命名", "删除")) { _, which ->
                when (which) {
                    0 -> onUserRename(user)
                    1 -> onUserDelete(user)
                }
            }
            .show()
    }

    inner class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val nameView: TextView = view.findViewById(R.id.tvUserName)

        fun bind(user: User?) {
            nameView.text = user?.name ?: "➕ 添加新用户"
            itemView.setOnClickListener { onUserClick(user) }

            if (user != null) {
                itemView.setOnLongClickListener {
                    showGroupDetailsDialog(it.context, user)
                    true
                }
            } else {
                itemView.setOnLongClickListener(null)
            }
        }
    }
}
