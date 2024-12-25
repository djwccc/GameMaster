package com.example.gamemaster

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class EditMatchFragment : Fragment() {

    private lateinit var refereeSpinner: Spinner
    private lateinit var teamASpinner: Spinner
    private lateinit var teamBSpinner: Spinner
    private lateinit var timeSpinner: Spinner
    private lateinit var btnSave: Button

    private lateinit var match: KnockoutMatchModel
    private lateinit var tournament: TournamentModel

    companion object {
        fun newInstance(tournament: TournamentModel, match: KnockoutMatchModel): EditMatchFragment {
            val fragment = EditMatchFragment()
            val args = Bundle()
            args.putParcelable("tournament", tournament)
            args.putParcelable("match", match)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            tournament = it.getParcelable("tournament") ?: TournamentModel("","","","","","")
            match = it.getParcelable("match")!!
        }
    }

    // 创建视图
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // 获取传递的 TournamentModel 对象
        arguments?.let {
            tournament = it.getParcelable("tournament")!!
        }

        return inflater.inflate(R.layout.fragment_edit_match, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 初始化 Spinner
        refereeSpinner = view.findViewById(R.id.spinner_referee)
        teamASpinner = view.findViewById(R.id.spinner_team_a)
        teamBSpinner = view.findViewById(R.id.spinner_team_b)
        timeSpinner = view.findViewById(R.id.spinner_time)
        btnSave = view.findViewById(R.id.btn_save_match)

        // 裁判员 Spinner
        val refereeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, tournament.referees.split(", "))
        refereeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        refereeSpinner.adapter = refereeAdapter

        // 队伍 Spinner
        val teamAAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, tournament.teams.split(", "))
        teamAAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        teamASpinner.adapter = teamAAdapter
        val teamBAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, tournament.teams.split(", "))
        teamBAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        teamBSpinner.adapter = teamBAdapter

        // 比赛时间 Spinner
        val timeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, tournament.matchTimes.split(", "))
        timeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        timeSpinner.adapter = timeAdapter

        // 设置当前比赛的信息到 Spinner
        val match = arguments?.getParcelable<KnockoutMatchModel>("match")
        refereeSpinner.setSelection(refereeAdapter.getPosition(match?.referee))
        teamASpinner.setSelection(teamAAdapter.getPosition(match?.teamA))
        teamBSpinner.setSelection(teamBAdapter.getPosition(match?.teamB))
        timeSpinner.setSelection(timeAdapter.getPosition(match?.matchTime))

        // 保存按钮监听
        btnSave.setOnClickListener {
            saveMatch()
        }
    }

    private fun saveMatch() {
        val refereeSpinner: Spinner = requireView().findViewById(R.id.spinner_referee)
        val teamASpinner: Spinner = requireView().findViewById(R.id.spinner_team_a)
        val teamBSpinner: Spinner = requireView().findViewById(R.id.spinner_team_b)
        val timeSpinner: Spinner = requireView().findViewById(R.id.spinner_time)

        val selectedReferee = refereeSpinner.selectedItem.toString()
        val selectedTeamA = teamASpinner.selectedItem.toString()
        val selectedTeamB = teamBSpinner.selectedItem.toString()
        val selectedTime = timeSpinner.selectedItem.toString()

        val updatedMatch = arguments?.getParcelable<KnockoutMatchModel>("match")?.copy(
            referee = selectedReferee,
            teamA = selectedTeamA,
            teamB = selectedTeamB,
            matchTime = selectedTime
        )

        updatedMatch?.let {
            updateMatchInSharedPreferences(it)  // 更新 SharedPreferences
            parentFragmentManager.setFragmentResult(
                "update_match_request",
                Bundle().apply { putParcelable("updated_match", it) }
            )
            parentFragmentManager.popBackStack()
        }
    }

    private fun updateMatchInSharedPreferences(updatedMatch: KnockoutMatchModel) {
        val sharedPreferences = requireContext().getSharedPreferences("tournament_data", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPreferences.getString("match_list", null)

        val type = object : TypeToken<MutableList<KnockoutMatchModel>>() {}.type
        val matchList: MutableList<KnockoutMatchModel> = if (json != null) {
            gson.fromJson(json, type)
        } else {
            mutableListOf()
        }

        // 查找并更新比赛信息
        val index = matchList.indexOfFirst { it.teamA == updatedMatch.teamA && it.teamB == updatedMatch.teamB }
        if (index != -1) {
            matchList[index] = updatedMatch // 更新数据
        }

        // 保存更新后的数据
        val editor = sharedPreferences.edit()
        editor.putString("match_list", gson.toJson(matchList))
        editor.apply()
    }
}
