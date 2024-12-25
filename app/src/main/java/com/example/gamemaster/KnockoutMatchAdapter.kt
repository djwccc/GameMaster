package com.example.gamemaster

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class KnockoutMatchAdapter(private var matchList: MutableList<KnockoutMatchModel>,
                        private val onEditMatch: (KnockoutMatchModel) -> Unit) :
    RecyclerView.Adapter<KnockoutMatchAdapter.KnockoutMatchViewHolder>() {

    class KnockoutMatchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val matchTime: TextView = itemView.findViewById(R.id.tv_match_time)
        val playingField: TextView = itemView.findViewById(R.id.tv_playing_field)
        val teams: TextView = itemView.findViewById(R.id.tv_teams)
        val referee: TextView = itemView.findViewById(R.id.tv_referee)
        val scores: TextView = itemView.findViewById(R.id.tv_scores)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KnockoutMatchViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_group_match, parent, false)
        return KnockoutMatchViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: KnockoutMatchViewHolder, position: Int) {
        val match = matchList[position]
        holder.matchTime.text = match.matchTime
        holder.playingField.text = "比赛场地：${match.playingField}"
        holder.teams.text = "${match.teamA} vs ${match.teamB}"
        holder.referee.text = "裁判员: ${match.referee}"
        if (match.scoreA!=null||match.scoreB!=null) {
            holder.scores.text = "${match.scoreA}:${match.scoreB}"
            holder.itemView.alpha = 0.7f
        } else {
            holder.scores.text = ""
            holder.itemView.alpha = 1.0f
        }

        holder.itemView.setOnClickListener {
            onEditMatch(match)
        }
    }

    override fun getItemCount() = matchList.size

}