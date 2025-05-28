package com.example.speakeridentification

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.speakeridentification.database.AppDatabase
import com.example.speakeridentification.database.GroupAdapter
import com.example.speakeridentification.database.UserGroup
import com.example.speakeridentification.database.UserGroupDao
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class GroupListActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var groupDao: UserGroupDao
    private lateinit var adapter: GroupAdapter
    private val groups = mutableListOf<UserGroup?>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.group_list)

        db = AppDatabase.getInstance(this)
        groupDao = db.userGroupDao()

        val recyclerView = findViewById<RecyclerView>(R.id.rvGroupList)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = GroupAdapter(groups,
            onGroupClick = { group ->
                if (group == null) showAddGroupDialog()
                else goToGroupDetail(group)
            },
            onGroupRename = { group -> showRenameGroupDialog(group) },
            onGroupDelete = { group -> confirmDeleteGroup(group) })

        recyclerView.adapter = adapter

        loadGroups()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun loadGroups() {
        lifecycleScope.launch {
            val storedGroups = groupDao.getAll()
            groups.clear()
            groups.addAll(storedGroups.map { UserGroup(it.id, it.name) })
            groups.add(null)
            adapter.notifyDataSetChanged()
        }
    }

    private fun showAddGroupDialog() {
        val editText = EditText(this)
        AlertDialog.Builder(this)
            .setTitle("添加新组")
            .setView(editText)
            .setPositiveButton("添加") { _, _ ->
                val groupName = editText.text.toString().trim()
                if (groupName.isNotEmpty()) {
                    addGroupToDatabase(groupName)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun addGroupToDatabase(name: String) {
        lifecycleScope.launch {
            groupDao.insert(UserGroup(name = name))
            loadGroups()
        }
        Toast.makeText(this, "成功添加用户组： ${name}", Toast.LENGTH_SHORT).show()
    }

    private fun goToGroupDetail(group: UserGroup) {
        Toast.makeText(this, "进入 ${group.name}", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, GroupDetailActivity::class.java)
        intent.putExtra("group_id", group.id)
        startActivity(intent)
    }

    private fun showRenameGroupDialog(group: UserGroup) {
        val editText = EditText(this).apply {
            setText(group.name)
        }

        AlertDialog.Builder(this)
            .setTitle("重命名用户组")
            .setView(editText)
            .setPositiveButton("确定") { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty()) {
                    lifecycleScope.launch {
                        groupDao.update(group.copy(name = newName))
                        loadGroups()
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun confirmDeleteGroup(group: UserGroup) {
        AlertDialog.Builder(this)
            .setTitle("确认删除")
            .setMessage("确定要删除用户组 \"${group.name}\" 吗？此操作不可恢复。")
            .setPositiveButton("删除") { _, _ ->
                lifecycleScope.launch {
                    groupDao.delete(group)
                    loadGroups()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }
}
