package com.example.gamemaster

import android.annotation.SuppressLint
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
import androidx.annotation.OptIn
import androidx.appcompat.widget.Toolbar
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class EditTournamentFragment : Fragment() {

    private lateinit var matchTypeSpinner: Spinner
    private lateinit var matchFormatSpinner: Spinner
    private lateinit var toolbar: Toolbar
    private var tournament: TournamentModel? = null
    private lateinit var matchRecyclerView: RecyclerView
    private lateinit var timeContainer: LinearLayout
    private lateinit var matchesList: MutableList<MatchModel>

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
    ): View? {
        // 获取传递的 TournamentModel 对象
        arguments?.let {
            tournament = it.getParcelable(ARG_TOURNAMENT)
        }

        return inflater.inflate(R.layout.fragment_edit_tournament, container, false) // 使用与添加赛程相同的布局
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        matchesList = mutableListOf()

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
                    deleteTournament()
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

        // 时间选择
        timeContainer = view.findViewById(R.id.timeContainer)
        val btnAddTime: Button = view.findViewById(R.id.btnAddTime)
        btnAddTime.setOnClickListener {
            showDateTimePicker()
        }

        // 加载赛程
        matchRecyclerView = view.findViewById(R.id.rv_matches)
        matchRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        val matchList = tournament?.generatedMatches ?: emptyList()

        // 将赛程按时间排序
        val sortedMatchList = matchList.sortedBy { it.matchTime }

        // 显示赛程
        val matchAdapter = MatchAdapter(sortedMatchList) { match ->
            openEditMatchFragment(match)
        }
        matchRecyclerView.adapter = matchAdapter

        // 初始化 UI 组件
        matchTypeSpinner = view.findViewById(R.id.spinnerMatchType)
        matchFormatSpinner = view.findViewById(R.id.spinnerMatchFormat)
        val teamsEditText: EditText = view.findViewById(R.id.editTextTeams)
        val refereeEditText: EditText = view.findViewById(R.id.editTextReferee)
        val btnAddTeam: Button = view.findViewById(R.id.btnAddTeam)
        val chipGroupTeams: ChipGroup = view.findViewById(R.id.chipGroupTeams)
        val btnAddReferee: Button = view.findViewById(R.id.btnAddReferee)
        val chipGroupReferees: ChipGroup = view.findViewById(R.id.chipGroupReferees)

        // 设置 Spinner 选项
        setupSpinners()

        // 显示当前赛程信息
        displayTournamentDetails(tournament)

        // 添加队伍和裁判员的点击事件
        setupChipGroupListeners(btnAddTeam, teamsEditText, chipGroupTeams)
        setupChipGroupListeners(btnAddReferee, refereeEditText, chipGroupReferees)
    }

    private fun setupSpinners() {
        val matchTypes = arrayOf("羽毛球", "足球", "篮球", "乒乓球", "排球", "毽球")
        val matchTypeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, matchTypes)
        matchTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        matchTypeSpinner.adapter = matchTypeAdapter

        val matchFormats = arrayOf("小组赛", "淘汰赛")
        val matchFormatAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, matchFormats)
        matchFormatAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        matchFormatSpinner.adapter = matchFormatAdapter
    }

    private fun displayTournamentDetails(tournament: TournamentModel?) {
        tournament?.let {
            val tournamentNameEditText: EditText = view?.findViewById(R.id.editTextTournamentName)!!
            tournamentNameEditText.setText(it.tournamentName)
            matchTypeSpinner.setSelection((matchTypeSpinner.adapter as ArrayAdapter<String>).getPosition(it.matchType))
            matchFormatSpinner.setSelection((matchFormatSpinner.adapter as ArrayAdapter<String>).getPosition(it.matchFormat))

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
    private fun saveTournament() {
        // 获取更新后的赛程信息
        val tournamentName = view?.findViewById<EditText>(R.id.editTextTournamentName)?.text.toString()
        val selectedMatchType = matchTypeSpinner.selectedItem.toString()
        val selectedMatchFormat = matchFormatSpinner.selectedItem.toString()

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

        // 更新赛程对象
        tournament?.let {
            it.tournamentName = tournamentName
            it.matchType = selectedMatchType
            it.matchFormat = selectedMatchFormat
            it.teams = teamsList
            it.referees = refereesList
            it.matchTimes = timesList

            // 更新 SharedPreferences 中的数据
            updateTournament(it)

            Toast.makeText(requireContext(), "赛程已更新", Toast.LENGTH_SHORT).show()
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
        private const val ARG_TOURNAMENT = "tournament"

        fun newInstance(tournament: TournamentModel): EditTournamentFragment {
            val fragment = EditTournamentFragment()
            val args = Bundle()
            args.putParcelable(ARG_TOURNAMENT, tournament) // 将 TournamentModel 作为参数传递
            fragment.arguments = args
            return fragment
        }
    }

    private fun openEditMatchFragment(match: MatchModel) {
        val fragment = EditMatchFragment.newInstance(match) // 传递当前赛程的信息
        requireActivity().supportFragmentManager.commit {
            replace(R.id.fragment_container, fragment)
            addToBackStack(null) // 允许用户返回到上一个 Fragment
        }
    }
}