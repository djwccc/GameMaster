package com.example.gamemaster

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.icu.util.Calendar
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
    private lateinit var timeEditText: EditText
    private var tournament: TournamentModel? = null

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
        val matchId = match.matchId
        val team1TextView: TextView = view.findViewById(R.id.editTextTeam1)
        val team2TextView: TextView = view.findViewById(R.id.editTextTeam2)
        timeEditText = view.findViewById(R.id.editTextMatchTime)
        val refereeEditText: EditText = view.findViewById(R.id.editTextReferee)
        val saveButton: Button = view.findViewById(R.id.buttonSave)

        // 显示当前的比赛信息
        team1TextView.text = match.teamA
        team2TextView.text = match.teamB
        timeEditText.setText(match.matchTime) // 显示比赛时间
        refereeEditText.setText(match.referee) // 显示裁判员信息

        timeEditText.setOnClickListener {
            showDateTimePicker()
        }

        // 设置保存按钮的点击事件
        saveButton.setOnClickListener {
            val updatedMatch = MatchModel(
                matchId = matchId,
                matchTime = timeEditText.text.toString(),
                referee = "裁判员", // 根据需要获取裁判员
                teamA = team1TextView.text.toString(),
                teamB = team2TextView.text.toString()
            )

            // 更新比赛信息
            updateMatchInSharedPreferences(updatedMatch)

            // 返回到 EditTournamentFragment
            requireActivity().supportFragmentManager.popBackStack()
        }
    }

    // 显示日期选择器
    @SuppressLint("DefaultLocale")
    private fun showDateTimePicker() {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(requireContext(), { _, year, month, dayOfMonth ->
            val timePickerDialog = TimePickerDialog(requireContext(), { _, hourOfDay, minute ->
                val selectedTime = String.format("%04d-%02d-%02d %02d:%02d", year, month + 1, dayOfMonth, hourOfDay, minute)
                timeEditText.setText(selectedTime)
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true)
            timePickerDialog.show()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))

        datePickerDialog.show()
    }

    private fun updateMatchInSharedPreferences(updatedMatch: MatchModel) {
        // 更新 SharedPreferences 中的比赛信息
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
        val index = matchList.indexOfFirst { it.teamA == updatedMatch.teamA && it.teamB == updatedMatch.teamB }
        if (index != -1) {
            matchList[index] = updatedMatch  // 更新
        }

        // 保存更新后的数据
        val editor = sharedPreferences.edit()
        editor.putString("match_list", gson.toJson(matchList))
        editor.apply()

        // 更新 generatedMatches 列表
        tournament?.generatedMatches = matchList // 更新 TournamentModel 中的赛程列表
        // 通知适配器数据已更新
//        MatchAdapter.MatchViewHolder // 假设你有一个 matchAdapter
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
