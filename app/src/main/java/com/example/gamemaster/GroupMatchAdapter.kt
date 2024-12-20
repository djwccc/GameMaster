package com.example.gamemaster

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class GroupMatchAdapter(private var matchList: MutableList<GroupMatchModel>,
                        private val onEditMatch: (GroupMatchModel) -> Unit) :
    RecyclerView.Adapter<GroupMatchAdapter.GroupMatchViewHolder>() {

    class GroupMatchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val matchTime: TextView = itemView.findViewById(R.id.tv_match_time)
        val playingField: TextView = itemView.findViewById(R.id.tv_playing_field)
        val teams: TextView = itemView.findViewById(R.id.tv_teams)
        val referee: TextView = itemView.findViewById(R.id.tv_referee)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupMatchViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_group_match, parent, false)
        return GroupMatchViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: GroupMatchViewHolder, position: Int) {
        val match = matchList[position]
        holder.matchTime.text = match.matchTime
        holder.playingField.text = "比赛场地：${match.playingField}"
        holder.teams.text = "${match.teamA} vs ${match.teamB}"
        holder.referee.text = "裁判员: ${match.referee}"

        holder.itemView.setOnClickListener {
            onEditMatch(match)
        }

    }

    override fun getItemCount() = matchList.size

}