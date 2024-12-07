package com.example.gamemaster

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.commit
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import android.Manifest
import android.widget.ImageView


class MainActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var tournamentAdapter: TournamentAdapter
    private val tournamentList = mutableListOf<TournamentModel>()
    private lateinit var fab: FloatingActionButton

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestStoragePermissions() // 请求存储权限

        // 初始化 RecyclerView
        recyclerView = findViewById(R.id.recyclerView)
        tournamentAdapter = TournamentAdapter(tournamentList) { selectedTournament ->
            openEditTournamentFragment(selectedTournament) // 打开编辑赛程的 Fragment
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = tournamentAdapter

        loadData() // 加载数据

        fab = findViewById(R.id.fab)
        fab.setOnClickListener {
            // 点击按钮时加载 Fragment
            val fragment = FormFragment()
            supportFragmentManager.commit {
                replace(R.id.fragment_container, fragment)
                addToBackStack(null) // 添加到返回栈
                findViewById<FrameLayout>(R.id.fragment_container).visibility = View.VISIBLE
                fab.visibility = View.GONE
                findViewById<androidx.appcompat.widget.LinearLayoutCompat>(R.id.linear_layout_compat).visibility =
                    View.GONE
                findViewById<ImageView>(R.id.imageBackground).alpha = 0F
            }
        }
    }

    fun onFragmentClose() {
        findViewById<FrameLayout>(R.id.fragment_container).visibility = View.GONE
        fab.visibility = View.VISIBLE
        findViewById<androidx.appcompat.widget.LinearLayoutCompat>(R.id.linear_layout_compat).visibility =
            View.VISIBLE
        findViewById<ImageView>(R.id.imageBackground).alpha = 0.5F
        // 重新加载数据
        loadData()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun loadData() {
        // 从 SharedPreferences 加载赛程数据
        val sharedPreferences = getSharedPreferences("tournament_data", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPreferences.getString("tournament_list", null)
        val type = object : TypeToken<MutableList<TournamentModel>>() {}.type
        val savedTournaments: MutableList<TournamentModel> =
            gson.fromJson(json, type) ?: mutableListOf()

        tournamentList.clear() // 清空旧数据
        tournamentList.addAll(savedTournaments)
        tournamentAdapter.notifyDataSetChanged() // 更新 RecyclerView
    }

    @SuppressLint("NotifyDataSetChanged")
    fun addTournament(tournament: TournamentModel) {
        tournamentList.add(tournament)
    }

    private fun requestStoragePermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            // 请求权限
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ),
                1
            )
        }
    }

    // 删除赛程
    @SuppressLint("NotifyDataSetChanged")
    fun deleteTournament(tournament: TournamentModel) {
        val index = tournamentList.indexOf(tournament)
        if (index != -1) {
            tournamentList.removeAt(index)
            tournamentAdapter.notifyItemRemoved(index) // 使用 notifyItemRemoved
        }
    }

    private fun openEditTournamentFragment(tournament: TournamentModel) {
        val fragment = EditTournamentFragment.newInstance(tournament)
        supportFragmentManager.commit {
            replace(R.id.fragment_container, fragment)
            addToBackStack(null)
            findViewById<FrameLayout>(R.id.fragment_container).visibility = View.VISIBLE
            fab.visibility = View.GONE
            findViewById<androidx.appcompat.widget.LinearLayoutCompat>(R.id.linear_layout_compat).visibility =
                View.GONE
            findViewById<ImageView>(R.id.imageBackground).alpha = 0F
        }
    }
}