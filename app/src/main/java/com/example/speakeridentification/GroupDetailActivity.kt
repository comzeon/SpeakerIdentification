package com.example.speakeridentification

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.speakeridentification.database.AppDatabase
import androidx.lifecycle.lifecycleScope
import com.example.speakeridentification.database.User
import com.example.speakeridentification.database.UserAdapter
import com.example.speakeridentification.database.UserDao
import kotlinx.coroutines.launch

class GroupDetailActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var userDao: UserDao
    private lateinit var adapter: UserAdapter
    private val users = mutableListOf<User?>()
    private var groupId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.group_detail)

        groupId = intent.getLongExtra("group_id", -1)
        if (groupId == -1L) {
            finish()
            return
        }

        db = AppDatabase.getInstance(this)
        userDao = db.userDao()

        val recyclerView = findViewById<RecyclerView>(R.id.rvUserList)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = UserAdapter(users,
            onUserClick = { user ->
                if (user == null) showAddUserDialog()
                else    goToUserRecording(user)
            },
            onUserRename = {user -> showRenameUserDialog(user)},
            onUserDelete = {user -> confirmDeleteUser(user)})

        recyclerView.adapter = adapter

        loadUsers()
    }

    private fun goToUserRecording(user: User) {
        Toast.makeText(this, "进入 ${user.name}", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, RecordingActivity::class.java)
        intent.putExtra("user_id", user.id)
        intent.putExtra("user_name", user.name)
        startActivity(intent)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun loadUsers() {
        lifecycleScope.launch {
            val userList = userDao.getUsersByGroupId(groupId)
            users.clear()
            users.addAll(userList)
            users.add(null)
            adapter.notifyDataSetChanged()
        }
    }

    private fun showAddUserDialog() {
        val editText = EditText(this)
        AlertDialog.Builder(this)
            .setTitle("添加用户")
            .setView(editText)
            .setPositiveButton("添加") { _, _ ->
                val userName = editText.text.toString().trim()
                if (userName.isNotEmpty()) {
                    addUserToDatabase(userName)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun addUserToDatabase(name: String) {
        lifecycleScope.launch {
            userDao.insert(User(name = name, groupId = groupId))
            loadUsers()
        }
        Toast.makeText(this, "成功添加用户： ${name}", Toast.LENGTH_SHORT).show()
    }

    private fun showRenameUserDialog(user: User) {
        val editText = EditText(this).apply {
            setText(user.name)
        }

        AlertDialog.Builder(this)
            .setTitle("重命名用户")
            .setView(editText)
            .setPositiveButton("确定") { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty()) {
                    lifecycleScope.launch {
                        userDao.update(user.copy(name = newName))
                        loadUsers()
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun confirmDeleteUser(user: User) {
        AlertDialog.Builder(this)
            .setTitle("确认删除")
            .setMessage("确定要删除用户组 \"${user.name}\" 吗？此操作不可恢复。")
            .setPositiveButton("删除") { _, _ ->
                lifecycleScope.launch {
                    userDao.delete(user)
                    loadUsers()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }
}
