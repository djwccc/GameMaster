package com.example.gamemaster

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MatchAdapter(private val matchList: List<MatchModel>,
                   private val onMatchClick: (MatchModel) -> Unit) :
    RecyclerView.Adapter<MatchAdapter.MatchViewHolder>() {

    class MatchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val matchTime: TextView = itemView.findViewById(R.id.tv_match_time)
        val teams: TextView = itemView.findViewById(R.id.tv_teams)
        val referee: TextView = itemView.findViewById(R.id.tv_referee)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MatchViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_match, parent, false)
        return MatchViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: MatchViewHolder, position: Int) {
        val match = matchList[position]
        holder.matchTime.text = match.matchTime
        holder.teams.text = "${match.teamA} vs ${match.teamB}"
        holder.referee.text = "裁判员: ${match.referee}"

        // 设置点击事件
        holder.itemView.setOnClickListener {
            onMatchClick(match) // 点击时回调
        }
    }

    override fun getItemCount() = matchList.size
}
