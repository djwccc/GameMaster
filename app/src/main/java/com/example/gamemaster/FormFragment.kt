package com.example.gamemaster

import android.annotation.SuppressLint
import android.app.TimePickerDialog
import android.content.Context
import android.icu.util.Calendar
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.core.util.Pair

class FormFragment : Fragment() {

    private lateinit var matchTypeSpinner: Spinner
    private lateinit var matchFormatSpinner: Spinner
    private lateinit var toolbar: Toolbar
    private lateinit var timeContainer: LinearLayout
    private lateinit var dateContainer: LinearLayout
    private lateinit var gradeInput: EditText
    private lateinit var classCountInput: EditText
    private val selectedMatchDates = mutableListOf<String>()
    private var selectedMatchTime: String? = null

    @Deprecated("Deprecated in Java")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.fragment_from_menu, menu) // 加载菜单资源
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true) // 确保菜单可见
        return inflater.inflate(R.layout.fragment_form, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar = view.findViewById(R.id.toolbar)
        (activity as AppCompatActivity).setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener {
            // 处理返回按钮点击事件
            (activity as? MainActivity)?.onFragmentClose() // 恢复 FloatingActionButton 和 AppBarLayout
            requireActivity().supportFragmentManager.popBackStack() // 退出 Fragment
        }

        timeContainer = view.findViewById(R.id.timeContainer)
        val btnAddTime: Button = view.findViewById(R.id.btnAddTime)
        btnAddTime.setOnClickListener {
            showTimePicker()
        }
        dateContainer = view.findViewById(R.id.dateContainer)
        val btnAddDate: Button = view.findViewById(R.id.btnAddDate)
        btnAddDate.setOnClickListener {
            showDatePicker()
        }

        // 初始化 UI 组件和逻辑
        matchTypeSpinner = view.findViewById(R.id.spinnerMatchType)
        matchFormatSpinner = view.findViewById(R.id.spinnerMatchFormat)
        val refereeEditText: EditText = view.findViewById(R.id.editTextReferee)
        gradeInput = view.findViewById(R.id.editTextGrade)
        classCountInput = view.findViewById(R.id.editTextClassCount)
        val btnAddReferee: Button = view.findViewById(R.id.btnAddReferee)
        val chipGroupReferees: ChipGroup = view.findViewById(R.id.chipGroupReferees)

        // 添加裁判员添加按钮点击事件
        btnAddReferee.setOnClickListener {
            val refereeName = refereeEditText.text.toString()
            if (refereeName.isNotBlank()) {
                // 创建 Chip 并设置文本
                val chip = Chip(context)
                chip.text = refereeName
                chip.isCloseIconVisible = true
                chip.setOnCloseIconClickListener {
                    chipGroupReferees.removeView(chip) // 删除 Chip
                }

                // 添加 Chip 到 ChipGroup
                chipGroupReferees.addView(chip)
                refereeEditText.text.clear() // 清空输入框
            }
        }

        // 设置比赛类型选择器
        val matchTypes = arrayOf("羽毛球", "足球", "篮球", "乒乓球", "排球", "毽球")
        val matchTypeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, matchTypes)
        matchTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        matchTypeSpinner.adapter = matchTypeAdapter

        // 设置赛制选择器
        val matchFormats = arrayOf("小组赛", "淘汰赛")
        val matchFormatAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, matchFormats)
        matchFormatAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        matchFormatSpinner.adapter = matchFormatAdapter

        // 添加保存按钮
        toolbar.inflateMenu(R.menu.fragment_from_menu)
        toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_save -> {
                    // 处理保存赛程逻辑
                    saveTournament()
                    true
                }
                else -> false
            }
        }
    }

    // 显示日期选择器
    private fun showDatePicker() {
        // 创建 MaterialDatePicker 实例
        val builder = MaterialDatePicker.Builder.dateRangePicker()
        builder.setSelection(Pair(MaterialDatePicker.todayInUtcMilliseconds(), MaterialDatePicker.todayInUtcMilliseconds())) // 设置默认日期范围为今天
        val datePicker = builder.build()

        // 设置日期选择完成后的回调
        datePicker.addOnPositiveButtonClickListener { selection ->
            val calendar = Calendar.getInstance()

            // 获取用户选择的日期范围
            val startDate = selection.first
            val endDate = selection.second

            // 获取开始和结束日期的 Calendar 实例
            calendar.timeInMillis = startDate
            val startDateFormatted = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

            calendar.timeInMillis = endDate
            val endDateFormatted = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

            // 获取并显示该范围内的所有日期
            val datesInRange = getDatesInRange(startDate, endDate)
            for (date in datesInRange) {
                selectedMatchDates.add(date)
                addDateView(date)
            }
        }

        // 显示 MaterialDatePicker
        datePicker.show(parentFragmentManager, datePicker.toString())
    }

    // 获取指定日期范围内的所有日期
    private fun getDatesInRange(startDate: Long, endDate: Long): List<String> {
        val dates = mutableListOf<String>()
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = startDate
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        while (calendar.timeInMillis <= endDate) {
            dates.add(sdf.format(calendar.time))
            calendar.add(Calendar.DAY_OF_MONTH, 1) // 向后推进一天
        }

        return dates
    }

    @SuppressLint("DefaultLocale")
    private fun showTimePicker() {
        val calendar = Calendar.getInstance()
        val timePickerDialog = TimePickerDialog(requireContext(), { _, hourOfDay, minute ->
            selectedMatchTime = String.format("%02d:%02d", hourOfDay, minute) // 格式化时间为 HH:mm
            addTimeView(selectedMatchTime!!)
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true)
        timePickerDialog.show()
    }

    private fun addDateView(date: String) {
        // 创建日期视图
        val dateView = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            val dateTextView = TextView(requireContext()).apply {
                text = date
            }
            addView(dateTextView)

            // 创建删除按钮
            val deleteButton = Button(requireContext()).apply {
                text = "删除"
            }

            addView(deleteButton)

            // 设置删除按钮的点击事件
            deleteButton.setOnClickListener {
                // 从容器中移除
                timeContainer.removeView(this)
                selectedMatchDates.remove(date)
            }
        }

        // 将日期视图添加到容器中
        dateContainer.addView(dateView)
    }

    private fun addTimeView(selectedTime: String) {
        // 创建时间视图
        val timeTextView = TextView(requireContext()).apply {
            text = selectedTime
        }
        // 创建删除按钮
        val deleteButton = Button(requireContext()).apply {
            text = "删除"
        }
        // 创建包含时间和删除按钮的布局
        val timeLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(timeTextView)
            addView(deleteButton)
        }
        // 点击删除按钮时移除整个时间布局
        deleteButton.setOnClickListener {
            timeContainer.removeView(timeLayout) // 从父视图中移除整个时间布局
        }
        // 将时间布局添加到容器中
        timeContainer.addView(timeLayout)
    }

    private fun saveTournament() {
        val tournamentName = view?.findViewById<EditText>(R.id.EditTournamentName)?.text.toString()
        val selectedMatchType = matchTypeSpinner.selectedItem.toString()
        val selectedMatchFormat = matchFormatSpinner.selectedItem.toString()
        val classCount = classCountInput.text.toString().toIntOrNull()
        val grade = gradeInput.text.toString()

        // 获取比赛时间
        val matchTimes = mutableListOf<String>()
        for (i in 0 until dateContainer.childCount) {
            val dateLayout = dateContainer.getChildAt(i) as LinearLayout
            val dateTextView = dateLayout.getChildAt(0) as TextView
            for (i in 0 until timeContainer.childCount) {
                val timeLayout = timeContainer.getChildAt(i) as LinearLayout
                val timeTextView = timeLayout.getChildAt(0) as TextView
                val matchDateTime = "${dateTextView.text.toString()} ${timeTextView.text.toString()}"
                matchTimes.add(matchDateTime)
            }
        }
        val timesList = matchTimes.joinToString(", ") // 用逗号分隔的时间列表

        // 生成班级队伍
        val teams = mutableListOf<String>()
        for (i in 1..classCount!!) {
            val teamName = "$grade $i 班"
            teams.add(teamName) // 添加队伍到列表
        }
        val teamsList = teams.joinToString(", ")

        // 获取裁判员列表
        val referees = mutableListOf<String>()
        val chipGroupReferees: ChipGroup = view?.findViewById(R.id.chipGroupReferees) ?: return
        for (i in 0 until chipGroupReferees.childCount) {
            val chip = chipGroupReferees.getChildAt(i) as Chip
            referees.add(chip.text.toString())
        }
        val refereesList = referees.joinToString(", ")

        // 生成赛程并保存
        val matchList = generateMatches(teams, referees, matchTimes)

        // 创建新的赛程对象
        val tournament = TournamentModel(
            tournamentName,
            selectedMatchType,
            selectedMatchFormat,
            teamsList,
            refereesList,
            timesList
        )
        tournament.generatedMatches = matchList // 保存赛程
        (activity as? MainActivity)?.addTournament(tournament)
        // 保存赛程到 SharedPreferences
        saveMatchesToSharedPreferences(tournament)
        // 保存数据
        saveData(tournament)
        // 反馈给用户
        Toast.makeText(requireContext(), "已添加赛程", Toast.LENGTH_LONG).show()
        (activity as? MainActivity)?.onFragmentClose()
        requireActivity().supportFragmentManager.popBackStack()
    }

    private fun saveData(tournament: TournamentModel) {
        val sharedPreferences =
            requireContext().getSharedPreferences("tournament_data", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPreferences.getString("tournament_list", null)

        val type = object : TypeToken<MutableList<TournamentModel>>() {}.type
        val tournamentList: MutableList<TournamentModel> = if (json != null) {
            gson.fromJson(json, type)
        } else {
            mutableListOf()
        }

        tournamentList.add(tournament) // 添加新的赛程信息

        // 将数据保存回 SharedPreferences
        val editor = sharedPreferences.edit()
        editor.putString("tournament_list", gson.toJson(tournamentList))
        editor.apply()
    }

    private fun generateMatches(teams: List<String>, referees: List<String>, matchTimes: List<String>): MutableList<MatchModel> {
        val matchList = mutableListOf<MatchModel>()
        val totalMatches = teams.size * (teams.size - 1) / 2 // 总比赛场次
        val availableMatchTimes = matchTimes.take(totalMatches) // 获取用户提供的可用比赛时间
        var matchId = 0
        var matchTimeIndex = 0

        // 先将班级按顺序排列，避免重复使用同一班级
        val teamPairs = mutableListOf<Pair<String, String>>()
        for (i in teams.indices) {
            for (j in i + 1 until teams.size) {
                teamPairs.add(Pair(teams[i], teams[j])) // 将所有班级配对
            }
        }

        // 现在我们需要将配对的班级错开安排
        val shuffledTeamPairs = teamPairs.shuffled() // 随机打乱班级配对
        val matchTimeGroups = mutableListOf<MutableList<Pair<String, String>>>()

        // 将所有配对分成不同的组别，每组间隔一定天数
        var groupIndex = 0
        for (pair in shuffledTeamPairs) {
            if (groupIndex >= matchTimeGroups.size) {
                matchTimeGroups.add(mutableListOf())
            }
            matchTimeGroups[groupIndex].add(pair)
            groupIndex = (groupIndex + 1) % availableMatchTimes.size // 每组比赛使用不同的时间
        }

        // 为每个比赛安排时间和裁判
        for (group in matchTimeGroups) {
            for (pair in group) {
                val teamA = pair.first
                val teamB = pair.second
                val matchTime = if (matchTimeIndex < availableMatchTimes.size) {
                    availableMatchTimes[matchTimeIndex] // 从用户提供的时间中取
                } else {
                    "未安排"
                }
                val referee = if (referees.isNotEmpty()) {
                    referees.random()  // 随机选择裁判员
                    } else "无裁判员"
                matchList.add(MatchModel(matchTime, referee, teamA, teamB, matchId.toString()))
                matchId ++
                matchTimeIndex++
            }
        }

        return matchList.sortedBy { it.matchTime }.toMutableList() // 根据 matchTime 排序
    }

    private fun saveMatchesToSharedPreferences(tournament: TournamentModel) {
        val sharedPreferences = requireContext().getSharedPreferences("tournament_data", Context.MODE_PRIVATE)
        val gson = Gson()
        val editor = sharedPreferences.edit()
        val json = gson.toJson(tournament)
        editor.putString("tournament", json)
        editor.apply()
    }
}