package com.example.gamemaster

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class EditMatchFragment : Fragment() {
    private lateinit var match: MatchModel // 用于存储传递过来的比赛对象

    // 创建视图
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // 获取传递的比赛对象
        match = requireArguments().getParcelable(ARG_MATCH)!!

        // 返回 Fragment 的布局
        return inflater.inflate(R.layout.fragment_edit_match, container, false)
    }

    // 在这里初始化 UI 组件，显示比赛信息
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 初始化 UI 组件
        val team1TextView: TextView = view.findViewById(R.id.editTextTeam1)
        val team2TextView: TextView = view.findViewById(R.id.editTextTeam2)
        val timeEditText: EditText = view.findViewById(R.id.editTextMatchTime)
        val refereeEditText: EditText = view.findViewById(R.id.editTextReferee)
        val saveButton: Button = view.findViewById(R.id.buttonSave)

        // 显示当前的比赛信息
        team1TextView.text = match.teamA
        team2TextView.text = match.teamB
        timeEditText.setText(match.matchTime) // 显示比赛时间
        refereeEditText.setText(match.referee) // 显示裁判员信息

        // 设置保存按钮的点击事件
        saveButton.setOnClickListener {
            saveMatch(timeEditText.text.toString(), refereeEditText.text.toString())
        }
    }

    private fun saveMatch(newTime: String, newReferee: String) {
        // 更新比赛信息
        match.matchTime = newTime
        match.referee = newReferee

        // 保存更新后的比赛信息
        updateMatchInSharedPreferences()

        // 关闭当前 Fragment
        requireActivity().supportFragmentManager.popBackStack()
    }

    private fun updateMatchInSharedPreferences() {
        // 更新 SharedPreferences 中的比赛信息
        // 这里需要加载当前的比赛列表，更新相应的比赛
        val sharedPreferences = requireContext().getSharedPreferences("tournament_data", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPreferences.getString("match_list", null)

        val type = object : TypeToken<MutableList<MatchModel>>() {}.type
        val matchList: MutableList<MatchModel> = if (json != null) {
            gson.fromJson(json, type)
        } else {
            mutableListOf()
        }

        // 查找并更新比赛信息
        val index = matchList.indexOfFirst { it.teamA == match.teamA && it.teamB == match.teamB }
        if (index != -1) {
            matchList[index] = match // 更新
        }

        // 保存更新后的数据
        val editor = sharedPreferences.edit()
        editor.putString("match_list", gson.toJson(matchList))
        editor.apply()
    }

    companion object {
        private const val ARG_MATCH = "match"

        fun newInstance(match: MatchModel): EditMatchFragment {
            val fragment = EditMatchFragment()
            val args = Bundle()
            args.putParcelable(ARG_MATCH, match) // 将 MatchModel 作为参数传递
            fragment.arguments = args
            return fragment
        }
    }
}
