package com.example.gamemaster

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.icu.util.Calendar
import android.os.Bundle
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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gamemaster.databinding.FragmentEditTournamentBinding
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class EditTournamentFragment : Fragment() {

    private lateinit var matchTypeSpinner: Spinner
    private lateinit var playingFieldsContainer: LinearLayout
    private lateinit var btnAddPlayingField: Button
    private lateinit var toolbar: Toolbar
    private var tournament: TournamentModel? = null
    private lateinit var matchRecyclerView: RecyclerView
    private lateinit var timeContainer: LinearLayout
    private lateinit var matchAdapter: GroupMatchAdapter
    private lateinit var btnGenerateGroup: Button
    private lateinit var rvGroups: RecyclerView
    private lateinit var tvRanking: TextView
    private val rankingList = mutableListOf<TeamStats>()
    private var teamStats = mutableMapOf<String, TeamStats>()
    private var _binding: FragmentEditTournamentBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true) // 允许该 Fragment 使用菜单
    }

    @Deprecated("Deprecated in Java")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_form, menu) // 加载菜单资源
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // 获取传递的 TournamentModel 对象
        arguments?.let {
            tournament = it.getParcelable(ARG_TOURNAMENT)
        }
        _binding = FragmentEditTournamentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar = view.findViewById(R.id.toolbar)
        (activity as AppCompatActivity).setSupportActionBar(toolbar)
        // 添加保存按钮
        toolbar.inflateMenu(R.menu.menu_form) // 确保菜单存在
        toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_save -> {
                    // 处理保存赛程逻辑
                    saveTournament()
                    true
                }
                R.id.action_delete -> {
                    // 处理删除赛程逻辑
                    AlertDialog.Builder(requireContext())
                        .setTitle("确认删除赛程?")
                        .setMessage("您确定要将此赛程全部删除吗？删除后不可恢复。")
                        .setPositiveButton("确定") { _, _ ->
                            deleteTournament()
                        }
                        .setNegativeButton("取消", null)
                        .show()
                    true
                }
                else -> false
            }
        }
        toolbar.setNavigationOnClickListener {
            // 处理返回按钮点击事件
            (activity as? MainActivity)?.onFragmentClose() // 恢复 FloatingActionButton 和 AppBarLayout
            requireActivity().supportFragmentManager.popBackStack() // 退出 Fragment
        }

        // 小组赛分组
        rvGroups = view.findViewById(R.id.rv_groups)
        rvGroups.layoutManager = LinearLayoutManager(requireContext())
        matchRecyclerView = view.findViewById(R.id.rvMatches)
        btnGenerateGroup = view.findViewById(R.id.btn_generate_group)

        matchAdapter = GroupMatchAdapter(tournament!!.generatedMatches ?: mutableListOf(),
            {match -> showGroupScoreInputDialog(match)})
        val matches = tournament!!.generatedMatches!!.toMutableList()
        val isGroupFinished = matches.all { it.isFinished } && matches.isNotEmpty()
        // 显示当前 Tournament 的赛程
        tournament?.let {
            val groups = it.groups
            // 判断是否进入了淘汰赛
            if (isGroupFinished) {
                btnGenerateGroup.visibility = View.GONE
                rvGroups.visibility = View.GONE
                // 刷新 RecyclerView 显示淘汰赛赛程
                val matchAdapter =
                    KnockoutMatchAdapter(tournament!!.knockoutMatches ?: mutableListOf(),
                        { match -> showKnockoutScoreInputDialog(match) })
                binding.rvMatches.layoutManager = LinearLayoutManager(requireContext())
                binding.rvMatches.adapter = matchAdapter
            }
            else {
                if (matches.isNotEmpty() && !groups.isNullOrEmpty()) {
                    Toast.makeText(requireContext(), "已加载赛程", Toast.LENGTH_SHORT).show()
                    btnGenerateGroup.visibility = View.GONE
                    val matchAdapter =
                        GroupMatchAdapter(tournament!!.generatedMatches ?: mutableListOf(),
                            { match -> showGroupScoreInputDialog(match) })
                    binding.rvMatches.layoutManager = LinearLayoutManager(requireContext())
                    binding.rvMatches.adapter = matchAdapter
                    val groupAdapter = GroupAdapter(tournament!!.groups ?: emptyMap())
                    binding.rvGroups.layoutManager = LinearLayoutManager(requireContext())
                    binding.rvGroups.adapter = groupAdapter

                    val sharedPreferences = requireContext().getSharedPreferences(
                        "tournament_prefs",
                        Context.MODE_PRIVATE
                    )
                    val editor = sharedPreferences.edit()
                    editor.putBoolean("isGroupGenerated_${tournament?.tournamentName}", false)
                    editor.apply()
                }
                    else if (matches.isEmpty() && groups.isNullOrEmpty()) {
                    btnGenerateGroup.visibility = View.VISIBLE
                    btnGenerateGroup.setOnClickListener {
                        val generateGroups =
                            generateGroups(tournament!!.teams.split(", "))  // teams 为从 TournamentModel 中获取的参赛队伍列表
                        val generateMatches = generateGroupMatches(generateGroups)
                        tournament!!.groups = generateGroups
                        tournament!!.generatedMatches = generateMatches.toMutableList()
                        btnGenerateGroup.visibility = View.GONE
                        val matchAdapter =
                            GroupMatchAdapter(tournament!!.generatedMatches ?: mutableListOf(),
                                { match -> showGroupScoreInputDialog(match) })
                        binding.rvMatches.layoutManager = LinearLayoutManager(requireContext())
                        binding.rvMatches.adapter = matchAdapter
                        val groupAdapter = GroupAdapter(tournament!!.groups ?: emptyMap())
                        binding.rvGroups.layoutManager = LinearLayoutManager(requireContext())
                        binding.rvGroups.adapter = groupAdapter

                        val sharedPreferences = requireContext().getSharedPreferences(
                            "tournament_prefs",
                            Context.MODE_PRIVATE
                        )
                        val editor = sharedPreferences.edit()
                        editor.putBoolean("isGroupGenerated_${tournament?.tournamentName}", true)
                        editor.apply()
                    }
                }
                }
        }

        // 时间选择
        timeContainer = view.findViewById(R.id.timeContainer)
        val btnAddTime: Button = view.findViewById(R.id.btnAddTime)
        btnAddTime.setOnClickListener {
            showDateTimePicker()
        }

        // 初始化 UI 组件
        matchTypeSpinner = view.findViewById(R.id.spinnerMatchType)
        playingFieldsContainer = view.findViewById(R.id.playingFieldsContainer)
        btnAddPlayingField = view.findViewById(R.id.btnAddPlayingField)
        val teamsEditText: EditText = view.findViewById(R.id.editTextTeams)
        val refereeEditText: EditText = view.findViewById(R.id.editTextReferee)
        val btnAddTeam: Button = view.findViewById(R.id.btnAddTeam)
        val chipGroupTeams: ChipGroup = view.findViewById(R.id.chipGroupTeams)
        val btnAddReferee: Button = view.findViewById(R.id.btnAddReferee)
        val chipGroupReferees: ChipGroup = view.findViewById(R.id.chipGroupReferees)
        tvRanking = view.findViewById(R.id.tvRanking)

        // 动态添加比赛场地
        btnAddPlayingField.setOnClickListener {
            addPlayingFieldInput()
        }
        // 加载已有的比赛场地
        loadPlayingFields()

        // 设置 Spinner 选项
        setupSpinners()

        // 显示当前赛程信息
        displayTournamentDetails(tournament!!)

        // 添加队伍和裁判员的点击事件
        setupChipGroupListeners(btnAddTeam, teamsEditText, chipGroupTeams)
        setupChipGroupListeners(btnAddReferee, refereeEditText, chipGroupReferees)
    }

    private fun addPlayingFieldInput(fieldName: String = "") {
        val fieldLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(0, 8, 0, 8)
        }

        val editText = EditText(requireContext()).apply {
            hint = "请输入比赛场地名称"
            setText(fieldName)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val deleteButton = Button(requireContext()).apply {
            text = "删除"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener {
                playingFieldsContainer.removeView(fieldLayout)
            }
        }

        fieldLayout.addView(editText)
        fieldLayout.addView(deleteButton)
        playingFieldsContainer.addView(fieldLayout)
    }

    private fun loadPlayingFields() {
        val playingFields = tournament?.playingFields?.split(", ") ?: emptyList()
        for (field in playingFields) {
            addPlayingFieldInput(field)
        }
    }

    private fun setupSpinners() {
        val matchTypes = arrayOf("羽毛球", "足球", "篮球", "乒乓球", "排球", "毽球")
        val matchTypeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, matchTypes)
        matchTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        matchTypeSpinner.adapter = matchTypeAdapter
    }

    private fun displayTournamentDetails(tournament: TournamentModel) {
        val tournamentNameEditText: EditText = view?.findViewById(R.id.editTextTournamentName)!!
        tournament.let {
            tournamentNameEditText.setText(it.tournamentName)
            matchTypeSpinner.setSelection((matchTypeSpinner.adapter as ArrayAdapter<String>).getPosition(it.matchType))

            // 分析参与队伍和裁判员
            val teams = it.teams.split(", ").toMutableList()
            val referees = it.referees.split(", ").toMutableList()

            teams.forEach { team ->
                addChipToGroup(team, view?.findViewById(R.id.chipGroupTeams)!!)
            }
            referees.forEach { referee ->
                addChipToGroup(referee, view?.findViewById(R.id.chipGroupReferees)!!)
            }

            // 创建时间视图
            val times = it.matchTimes.split(", ").toMutableList()
            times.forEach { time ->
                val timeTextView = TextView(requireContext()).apply {
                    text = time
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
        }
    }

    private fun addChipToGroup(text: String, chipGroup: ChipGroup) {
        val chip = Chip(context)
        chip.text = text
        chip.isCloseIconVisible = true
        chip.setOnCloseIconClickListener {
            chipGroup.removeView(chip)
        }
        chipGroup.addView(chip)
    }

    private fun setupChipGroupListeners(btn: Button, editText: EditText, chipGroup: ChipGroup) {
        btn.setOnClickListener {
            val name = editText.text.toString()
            if (name.isNotBlank()) {
                addChipToGroup(name, chipGroup)
                editText.text.clear() // 清空输入框
            }
        }
    }

    // 显示日期选择器
    @SuppressLint("DefaultLocale")
    private fun showDateTimePicker() {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(requireContext(), { _, year, month, dayOfMonth ->
            val timePickerDialog = TimePickerDialog(requireContext(), { _, hourOfDay, minute ->
                val selectedTime = String.format("%04d-%02d-%02d %02d:%02d", year, month + 1, dayOfMonth, hourOfDay, minute)
                addTimeView(selectedTime)
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true)
            timePickerDialog.show()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))

        datePickerDialog.show()
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

    // 保存修改后的赛程
    @SuppressLint("CutPasteId")
    private fun saveTournament() {
        val sharedPreferences = requireContext().getSharedPreferences("tournament_prefs", Context.MODE_PRIVATE)
        val isGroupGenerated = sharedPreferences.getBoolean("isGroupGenerated_${tournament?.tournamentName}", false)

        // 获取更新后的赛程信息
        val tournamentName = view?.findViewById<EditText>(R.id.editTextTournamentName)?.text.toString()
        val selectedMatchType = matchTypeSpinner.selectedItem.toString()
        var isChangedInfo = false
        if (
            tournamentName!=tournament!!.tournamentName
            || selectedMatchType!=tournament!!.matchType
            || isGroupGenerated){
            isChangedInfo = true
        }
        // 获取原信息，判断是否有改动
        arguments?.let {
            tournament = it.getParcelable(ARG_TOURNAMENT)
        }
        val originTeams = tournament?.teams?.split(", ")?.toMutableList()
        val originReferees = tournament?.referees?.split(", ")?.toMutableList()
        val originTimes = tournament?.matchTimes?.split(", ")?.toMutableList()

        // 获取参与的队伍
        val teams = mutableListOf<String>()
        val chipGroupTeams: ChipGroup = view?.findViewById(R.id.chipGroupTeams)!!
        for (i in 0 until chipGroupTeams.childCount) {
            val chip = chipGroupTeams.getChildAt(i) as Chip
            teams.add(chip.text.toString())
        }
        val teamsList = teams.joinToString(", ") // 用逗号分隔的队伍列表

        // 获取裁判员列表
        val referees = mutableListOf<String>()
        val chipGroupReferees: ChipGroup = view?.findViewById(R.id.chipGroupReferees)!!
        for (i in 0 until chipGroupReferees.childCount) {
            val chip = chipGroupReferees.getChildAt(i) as Chip
            referees.add(chip.text.toString())
        }
        val refereesList = referees.joinToString(", ") // 用逗号分隔的队伍列表

        // 获取比赛时间
        val matchTimes = mutableListOf<String>()
        for (i in 0 until timeContainer.childCount) {
            val timeLayout = timeContainer.getChildAt(i) as LinearLayout
            val timeTextView = timeLayout.getChildAt(0) as TextView
            matchTimes.add(timeTextView.text.toString())
        }
        val timesList = matchTimes.joinToString(", ") // 用逗号分隔的时间列表

        // 获取比赛场地
        val playingFields = mutableListOf<String>()
        for (i in 0 until playingFieldsContainer.childCount) {
            val fieldLayout = playingFieldsContainer.getChildAt(i) as LinearLayout
            val editText = fieldLayout.getChildAt(0) as EditText
            val fieldName = editText.text.toString()
            if (fieldName.isNotEmpty()) {
                playingFields.add(fieldName)
            }
        }
        val playingFieldsList = playingFields.joinToString(", ")

        if (timesList != originTimes?.joinToString(", ")
            || refereesList != originReferees?.joinToString(", ")
            || teamsList != originTeams?.joinToString(", ")
            || playingFieldsList != tournament?.playingFields){
            isChangedInfo = true
        }

        // 动态设置提示信息
        val message = if (isGroupGenerated) {
            "比赛队伍已经进行分组，赛程已经生成，是否保存？"
        } else {
            "您已修改赛程信息，是否保存？现有数据将被覆盖，比赛将重置，无法恢复。"
        }

        if (isChangedInfo){
            AlertDialog.Builder(requireContext())
                .setTitle("保存修改?")
                .setMessage(message)
                .setPositiveButton("保存") { _, _ ->
                    // 更新赛程对象
                    tournament?.let {
                        it.matchType = selectedMatchType
                        it.playingFields = playingFieldsList
                        it.teams = teamsList
                        it.referees = refereesList
                        it.matchTimes = timesList
                        if (!isGroupGenerated) {
                            it.groups = mapOf() // 清空
                            it.generatedMatches = mutableListOf()
                        }
                        // 更新 SharedPreferences 中的数据
                        updateTournament(it)

                        Toast.makeText(requireContext(), "赛程已更新", Toast.LENGTH_SHORT).show()
                        (activity as? MainActivity)?.onFragmentClose() // 恢复 FloatingActionButton 和 AppBarLayout
                        requireActivity().supportFragmentManager.popBackStack() // 退出 Fragment
                    }
                }
                .setNegativeButton("取消", null)
                .show()
        } else {
            (activity as? MainActivity)?.onFragmentClose() // 恢复 FloatingActionButton 和 AppBarLayout
            requireActivity().supportFragmentManager.popBackStack() // 退出 Fragment
        }
    }

    // 删除赛程
    private fun deleteTournament() {
        tournament?.let { tournamentToDelete ->
            val sharedPreferences = requireContext().getSharedPreferences("tournament_data", Context.MODE_PRIVATE)
            val gson = Gson()
            val json = sharedPreferences.getString("tournament_list", null)

            val type = object : TypeToken<MutableList<TournamentModel>>() {}.type
            val tournamentList: MutableList<TournamentModel> = if (json != null) {
                gson.fromJson(json, type)
            } else {
                mutableListOf()
            }

            // 删除相应的赛程
            tournamentList.removeIf { it.tournamentName == tournamentToDelete.tournamentName }

            // 更新 SharedPreferences 中的数据
            val editor = sharedPreferences.edit()
            editor.putString("tournament_list", gson.toJson(tournamentList))
            editor.apply()
            editor.putString("group_match_list", gson.toJson(tournamentList))
            editor.apply()

            Toast.makeText(requireContext(), "赛程已删除", Toast.LENGTH_SHORT).show()

            // 关闭当前 Fragment 并返回主界面
            (activity as? MainActivity)?.deleteTournament(tournamentToDelete)
            (activity as? MainActivity)?.onFragmentClose()
            requireActivity().supportFragmentManager.popBackStack()
        }
    }

    // 修改后刷新赛程
    private fun updateTournament(updatedTournament: TournamentModel) {
        val sharedPreferences = requireContext().getSharedPreferences("tournament_data", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPreferences.getString("tournament_list", null)

        val type = object : TypeToken<MutableList<TournamentModel>>() {}.type
        val tournamentList: MutableList<TournamentModel> = if (json != null) {
            gson.fromJson(json, type)
        } else {
            mutableListOf()
        }

        // 查找并更新赛程信息
        val index = tournamentList.indexOfFirst { it.tournamentName == updatedTournament.tournamentName }
        if (index != -1) {
            tournamentList[index] = updatedTournament // 更新
        }

        // 保存更新后的数据
        val editor = sharedPreferences.edit()
        editor.putString("tournament_list", gson.toJson(tournamentList))
        editor.apply()
    }

    companion object {
        const val ARG_TOURNAMENT = "tournament"

        fun newInstance(tournament: TournamentModel): EditTournamentFragment {
            val fragment = EditTournamentFragment()
            val args = Bundle()
            args.putParcelable(ARG_TOURNAMENT, tournament) // 将 TournamentModel 作为参数传递
            fragment.arguments = args
            return fragment
        }
    }

    // 抽签分组的函数
    private fun generateGroups(teams: List<String>): Map<String, List<String>> {
        val shuffledTeams = teams.shuffled()
        val groups = mutableMapOf<String, MutableList<String>>()

        // 按顺序分组：A组、B组、C组、D组
        val groupNames = arrayOf("A组", "B组", "C组", "D组")

        for ((index, team) in shuffledTeams.withIndex()) {
            val groupIndex = index % groupNames.size
            val groupName = groupNames[groupIndex]
            if (!groups.containsKey(groupName)) {
                groups[groupName] = mutableListOf()
            }
            groups[groupName]?.add(team)
        }

        tournament!!.groups = groups
        return groups
    }

    // 生成小组赛赛程
    private fun generateGroupMatches(groups: Map<String, List<String>>): List<GroupMatchModel> {
        val matchList = mutableListOf<GroupMatchModel>()
        val matchTimes = tournament!!.matchTimes.split(", ").toMutableList()
        val playingFields = tournament!!.playingFields.split(", ")
        val referees = tournament!!.referees.split(", ")
        var matchId = 0
        val groupNames = groups.keys.toList()
        var timeIndex = 0

        // 遍历每个小组，生成小组赛赛程
        for (groupName in groupNames) {
            val groupTeams = groups[groupName] ?: continue
            for (i in groupTeams.indices) {
                for (j in i + 1 until groupTeams.size) {
                    val teamA = groupTeams[i]
                    val teamB = groupTeams[j]
                    val matchTime = matchTimes.getOrElse(timeIndex % matchTimes.size) { "未安排" }
                    val playingField = playingFields.random()
                    val referee = referees.random()

                    // 创建比赛对象
                    val matchModel = GroupMatchModel(
                        group = groupName,
                        matchTime = matchTime,
                        playingField = playingField,
                        referee = referee,
                        teamA = teamA,
                        teamB = teamB,
                        matchId = matchId.toString()
                    )

                    matchList.add(matchModel)
                    timeIndex = (timeIndex + 1) % matchTimes.size
                    updateTournament(tournament!!)
                    matchId ++
                }
            }
        }
        tournament!!.generatedMatches!!.clear()
        tournament!!.generatedMatches!!.addAll(matchList)
        return matchList.sortedBy { it.matchTime }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun showEditMatchDialog(match: GroupMatchModel) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_match, null)
        val editTime = dialogView.findViewById<EditText>(R.id.editMatchTime)
        val editField = dialogView.findViewById<EditText>(R.id.editPlayingField)

        editTime.setText(match.matchTime)
        editField.setText(match.playingField)

        AlertDialog.Builder(requireContext())
            .setTitle("编辑比赛")
            .setView(dialogView)
            .setPositiveButton("保存") { _, _ ->
                val newTime = editTime.text.toString()
                val newField = editField.text.toString()
                if (newTime.isNotEmpty() && newField.isNotEmpty()) {
                    match.matchTime = newTime
                    match.playingField = newField
                    tournament?.generatedMatches?.find { it.matchId == match.matchId }?.apply {
                        matchTime = newTime
                        playingField = newField
                        updateMatchInTournament(tournament!!, match.matchId, matchTime, playingField) // 保存更新后的比赛
                    }
                    binding.rvMatches.layoutManager = LinearLayoutManager(requireContext())
                    binding.rvMatches.adapter = matchAdapter
                } else {
                    Toast.makeText(requireContext(), "时间和场地不能为空", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun updateMatchInTournament(tournament: TournamentModel, matchId: String, newTime: String, newField: String) {
        // 在生成的比赛列表中找到对应的比赛
        val match = tournament.generatedMatches?.find { it.matchId == matchId }

        // 如果找到了对应的比赛，更新它的时间和场地
        if (match != null) {
            match.matchTime = newTime
            match.playingField = newField

            // 更新 TournamentModel 中的 generatedMatches
            val updatedTournament = tournament.copy(generatedMatches = tournament.generatedMatches)

            // 更新 SharedPreferences 中的 TournamentModel 数据
            saveTournamentToSharedPreferences(updatedTournament)
        } else {
            Toast.makeText(requireContext(), "比赛未找到", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveTournamentToSharedPreferences(tournament: TournamentModel) {
        val sharedPreferences = requireContext().getSharedPreferences("tournament_data", Context.MODE_PRIVATE)
        val gson = Gson()

        // 将更新后的 TournamentModel 转换为 JSON 字符串
        val tournamentJson = gson.toJson(tournament)

        // 保存到 SharedPreferences
        val editor = sharedPreferences.edit()
        editor.putString("tournament", tournamentJson)
        editor.apply()

        Toast.makeText(requireContext(), "赛程已更新", Toast.LENGTH_SHORT).show()
    }

    private fun showGroupScoreInputDialog(match: GroupMatchModel) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_input_score, null)
        val scoreAEditText = dialogView.findViewById<EditText>(R.id.editTextScoreA)
        val scoreBEditText = dialogView.findViewById<EditText>(R.id.editTextScoreB)

        AlertDialog.Builder(requireContext())
            .setTitle("输入比分")
            .setView(dialogView)
            .setPositiveButton("保存") { _, _ ->
                val scoreA = scoreAEditText.text.toString().toIntOrNull()
                val scoreB = scoreBEditText.text.toString().toIntOrNull()

                if (scoreA != null && scoreB != null) {
                    match.scoreA = scoreA
                    match.scoreB = scoreB
                    match.isFinished = true

                    // 更新RecyclerView
                    binding.rvMatches.layoutManager = LinearLayoutManager(requireContext())
                    binding.rvMatches.adapter = matchAdapter

                    // 查看是否结束小组赛
                    checkGroupStageCompletion()

                    // 保存到 SharedPreferences 和 TournamentModel
                    saveGroupMatchData(match)
                    updateTournament(tournament!!)
                } else {
                    Toast.makeText(requireContext(), "请输入有效的比分", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }


    private fun showKnockoutScoreInputDialog(match: KnockoutMatchModel) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_input_score, null)
        val scoreAEditText = dialogView.findViewById<EditText>(R.id.editTextScoreA)
        val scoreBEditText = dialogView.findViewById<EditText>(R.id.editTextScoreB)

        AlertDialog.Builder(requireContext())
            .setTitle("输入比分")
            .setView(dialogView)
            .setPositiveButton("保存") { _, _ ->
                val scoreA = scoreAEditText.text.toString().toIntOrNull()
                val scoreB = scoreBEditText.text.toString().toIntOrNull()

                if (scoreA != null && scoreB != null) {
                    match.scoreA = scoreA
                    match.scoreB = scoreB
                    match.isFinished = true
                    // 更新RecyclerView
                    val matchAdapter =
                        KnockoutMatchAdapter(tournament!!.knockoutMatches ?: mutableListOf(),
                            { match -> showKnockoutScoreInputDialog(match) })
                    binding.rvMatches.layoutManager = LinearLayoutManager(requireContext())
                    binding.rvMatches.adapter = matchAdapter
                    val matches = tournament!!.knockoutMatches!!

                    if (matches.all { it.isFinished } && matches.isNotEmpty()) {
                        matches.forEach { _ ->
                            updateTeamStats(match.teamA, match.teamB, match.scoreA!!,match.scoreB!!)
                        }

                        // 继续淘汰赛
                        if (matches.size > 1) onKnockoutRoundFinished(matches)
                        else {
                            // 结束比赛
                            AlertDialog.Builder(requireContext())
                                .setTitle("全部比赛已经结束")
                                .setMessage("全部比赛已结束，系统将输出比赛总排名。")
                                .setPositiveButton("我知道了") { _, _ ->
                                }
                                .show()
                            matchOver()
                        }

                    }
                    // 保存到 SharedPreferences 或 TournamentModel
                    saveKnockoutMatchData(match)
                    updateTournament(tournament!!)

                } else {
                    Toast.makeText(requireContext(), "请输入有效的比分", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun checkGroupStageCompletion() {
        val matches = tournament!!.generatedMatches!!.toMutableList()
        val isGroupFinished = matches.all { it.isFinished }

        if (isGroupFinished) {
            Toast.makeText(requireContext(), "小组赛已结束！", Toast.LENGTH_SHORT).show()

            // 进入淘汰赛
            AlertDialog.Builder(requireContext())
                .setTitle("小组赛结束")
                .setMessage("小组赛已结束，即将进入淘汰赛。")
                .setPositiveButton("我知道了") { _, _ ->
                }
                .show()

            // 进入淘汰赛逻辑
            proceedToKnockoutStage(matches)
        }
    }

    private fun getTeamsForKnockout(matches: MutableList<GroupMatchModel>): MutableList<String> {
        // 统计积分
        val teamScores = mutableMapOf<String, Int>()

        matches.forEach { match ->
            val scoreA = match.scoreA ?: 0
            val scoreB = match.scoreB ?: 0

            when {
                scoreA > scoreB -> {
                    teamScores[match.teamA] = teamScores.getOrDefault(match.teamA, 0) + 3
                }
                scoreB > scoreA -> {
                    teamScores[match.teamB] = teamScores.getOrDefault(match.teamB, 0) + 3
                }
                else -> {
                    teamScores[match.teamA] = teamScores.getOrDefault(match.teamA, 0) + 1
                    teamScores[match.teamB] = teamScores.getOrDefault(match.teamB, 0) + 1
                }
            }
        }

        // 按组筛选每组前两名
        val groupedTeams = matches.groupBy { it.group }
        val knockoutTeams = mutableListOf<String>()

        groupedTeams.forEach { (_, groupMatches) ->
            val groupScores = teamScores.filter { team ->
                groupMatches.any { it.teamA == team.key || it.teamB == team.key }
            }.toList().sortedByDescending { it.second } // 按积分降序排列

            knockoutTeams.addAll(groupScores.take(2).map { it.first }) // 每组取前两名
        }

        return knockoutTeams
    }

    private fun generateKnockoutMatches(teams: MutableList<String>): MutableList<KnockoutMatchModel> {
        val lastGroupMatchTime = tournament!!.generatedMatches!!.last().matchTime
        val knockoutMatches = mutableListOf<KnockoutMatchModel>()
        var matchTimes = tournament!!.matchTimes.split(", ")
        val processTimeIndex = matchTimes.indexOf(lastGroupMatchTime) + 1
        matchTimes = matchTimes.slice(processTimeIndex..<matchTimes.size)
        val playingFields = tournament!!.playingFields.split(", ")
        val referees = tournament!!.referees.split(", ")
        var matchId = 100
        var timeIndex = 0

        for (i in 0 until teams.size / 2) {
            val teamA = teams[i]
            val teamB = teams[teams.size - 1 - i]
            val matchTime = matchTimes.getOrElse(timeIndex % matchTimes.size) { "未安排" }
            val playingField = playingFields.random()
            val referee = referees.random()

            val match = KnockoutMatchModel(
                matchTime = matchTime,
                playingField = playingField,
                referee = referee,
                teamA = teamA,
                teamB = teamB,
                matchId = matchId.toString()
            )

            knockoutMatches.add(match)
            timeIndex = (timeIndex + 1) % matchTimes.size
            updateTournament(tournament!!)
            matchId ++
        }
        tournament!!.knockoutMatches!!.clear()
        tournament!!.knockoutMatches!!.addAll(knockoutMatches)
        return knockoutMatches.sortedBy { it.matchTime }.toMutableList()
    }

    private fun proceedToKnockoutStage(matches: MutableList<GroupMatchModel>) {
        val teams = getTeamsForKnockout(matches)

        // 初始化排名
        rankingList.clear()
        rankingList.addAll(teams.map { teamName ->
            TeamStats(teamName, 0)
        })

        val knockoutMatches = generateKnockoutMatches(teams)

        // 保存淘汰赛赛程
        saveKnockoutMatches(knockoutMatches)

        // 刷新 RecyclerView 显示淘汰赛赛程
        val matchAdapter = KnockoutMatchAdapter(tournament!!.knockoutMatches ?: mutableListOf() ,
            {match -> showKnockoutScoreInputDialog(match)})
        binding.rvMatches.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMatches.adapter = matchAdapter
        updateTournament(tournament!!)
    }

    private fun saveGroupMatchData(match: GroupMatchModel) {
        val sharedPreferences = requireContext().getSharedPreferences("tournament_data", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPreferences.getString("match_list", null)

        val type = object : TypeToken<MutableList<GroupMatchModel>>() {}.type
        val matchList: MutableList<GroupMatchModel> = if (json != null) {
            gson.fromJson(json, type)
        } else {
            mutableListOf()
        }

        val index = matchList.indexOfFirst { it.matchId == match.matchId }
        if (index != -1) {
            matchList[index] = match
        } else {
            matchList.add(match)
        }

        val editor = sharedPreferences.edit()
        editor.putString("match_list", gson.toJson(matchList))
        editor.apply()
    }

    private fun saveKnockoutMatches(matches: MutableList<KnockoutMatchModel>) {
        val sharedPreferences = requireContext().getSharedPreferences("tournament_data", Context.MODE_PRIVATE)
        val gson = Gson()

        val editor = sharedPreferences.edit()
        editor.putString("knockout_match_list", gson.toJson(matches))
        editor.apply()
    }

    private fun saveKnockoutMatchData(match: KnockoutMatchModel) {
        val sharedPreferences = requireContext().getSharedPreferences("tournament_data", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPreferences.getString("match_list", null)

        val type = object : TypeToken<MutableList<KnockoutMatchModel>>() {}.type
        val matchList: MutableList<KnockoutMatchModel> = if (json != null) {
            gson.fromJson(json, type)
        } else {
            mutableListOf()
        }

        val index = matchList.indexOfFirst { it.matchId == match.matchId }
        if (index != -1) {
            matchList[index] = match
        } else {
            matchList.add(match)
        }

        val editor = sharedPreferences.edit()
        editor.putString("match_list", gson.toJson(matchList))
        editor.apply()
    }

    private fun onKnockoutRoundFinished(matches: List<KnockoutMatchModel>) {
        // 获取胜者
        val winners = getWinners(matches)
        if (winners.isEmpty()) {
            Toast.makeText(requireContext(), "尚未完成所有比赛，无法生成下一轮赛程！", Toast.LENGTH_SHORT).show()
            return
        }
        // 进入淘汰赛
        AlertDialog.Builder(requireContext())
            .setTitle("进入下一轮淘汰赛")
            .setMessage("本轮淘汰赛已结束，即将进入下一轮淘汰赛。")
            .setPositiveButton("我知道了") { _, _ ->
            }
            .show()
        // 生成下一轮赛程
        val nextRound = generateNextRound(winners)
        // 更新淘汰赛赛程
        nextRound.forEach { _ ->
            val matchAdapter = KnockoutMatchAdapter(tournament!!.knockoutMatches ?: mutableListOf() ,
                {match -> showKnockoutScoreInputDialog(match)})
            binding.rvMatches.layoutManager = LinearLayoutManager(requireContext())
            binding.rvMatches.adapter = matchAdapter
            updateTournament(tournament!!)
        }
    }

    private fun getWinners(matches: List<KnockoutMatchModel>): List<String> {
        return matches.mapNotNull {
            if (it.scoreA != null && it.scoreB != null) {
                // 判断比分高的队伍
                if (it.scoreA!! > it.scoreB!!) it.teamA else it.teamB
            } else {
                null // 比赛未完成，不返回胜者
            }
        }
    }

    private fun generateNextRound(winners: List<String>): List<KnockoutMatchModel> {
        val progressMatchTime = tournament!!.knockoutMatches!!.last().matchTime
        val nextRoundMatches = mutableListOf<KnockoutMatchModel>()
        var matchTimes = tournament!!.matchTimes.split(", ")
        val processTimeIndex = matchTimes.indexOf(progressMatchTime) + 1
        matchTimes = matchTimes.slice(processTimeIndex..<matchTimes.size)
        val playingFields = tournament!!.playingFields.split(", ")
        val referees = tournament!!.referees.split(", ")
        var timeIndex = 0

        // 两两分组生成比赛
        for (i in winners.indices step 2) {
            if (i + 1 < winners.size) {
                val teamA = winners[i]
                val teamB = winners[winners.size - 1 - i]
                val matchTime = matchTimes.getOrElse(timeIndex % matchTimes.size) { "未安排" }
                val playingField = playingFields.random()
                val referee = referees.random()
                nextRoundMatches.add(
                    KnockoutMatchModel(
                        matchId = "KO_${System.currentTimeMillis()}_${i / 2}", // 比赛ID
                        teamA = teamA, // 胜者1
                        teamB = teamB, // 胜者2
                        referee = referee, // 示例裁判
                        playingField = playingField,
                        matchTime = matchTime
                    )
                )

                tournament!!.matchTimes = matchTimes.joinToString(", ")
                timeIndex = (timeIndex + 1) % matchTimes.size
                updateTournament(tournament!!)
            }
        }
        tournament!!.knockoutMatches!!.clear()
        tournament!!.knockoutMatches!!.addAll(nextRoundMatches)
        return nextRoundMatches.sortedBy { it.matchTime }
    }

    private fun updateTeamStats(team1: String,team2: String,score1: Int,score2: Int) {
        val stats1 = teamStats.getOrPut(team1) { TeamStats(teamName = team1) }
        val stats2 = teamStats.getOrPut(team2) { TeamStats(teamName = team2) }

        // 通过积分的方法辅助统计排名
        when {
            score1 > score2 -> stats1.points += 3
            score1 < score2 -> stats2.points += 3
            else -> {
                stats1.points += 1
                stats2.points += 1
            }
        }
    }

    private fun matchOver() {
        rvGroups.visibility = View.GONE
        matchRecyclerView.visibility = View.GONE
        tvRanking.visibility = View.VISIBLE
        rankingList.sortedByDescending { it.points } // 按积分降序排列

        tvRanking = TextView(requireContext()).apply {
            val rankingText = "最终排名：\n" + rankingList.mapIndexed { index, teamStats ->
                "${index + 1}. ${teamStats.teamName}"
            }.joinToString("\n")
            // 设置到 TextView
            tvRanking.text = rankingText
        }
    }
}