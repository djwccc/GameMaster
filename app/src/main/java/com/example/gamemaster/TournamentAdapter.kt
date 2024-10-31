package com.example.gamemaster

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TournamentAdapter(private var tournamentList: MutableList<TournamentModel>,
                        private val onTournamentClicked: (TournamentModel) -> Unit) :
    RecyclerView.Adapter<TournamentAdapter.ItemViewHolder>() {

    class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tournamentName: TextView = view.findViewById(R.id.tournamentName)
        val matchType: TextView = view.findViewById(R.id.matchType)
        val matchFormat: TextView = view.findViewById(R.id.matchFormat)
        val teams: TextView = view.findViewById(R.id.teams)
        val referees: TextView = view.findViewById(R.id.referees)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_tournament , parent, false)
        return ItemViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
//        Log.d("TournamentAdapter", "Binding item at position: $position")
        val tournament = tournamentList[position]
        holder.tournamentName.text = tournament.tournamentName
        holder.matchType.text = "比赛类型: ${tournament.matchType}"
        holder.matchFormat.text = "赛制: ${tournament.matchFormat}"
        holder.teams.text = "参与队伍: ${tournament.teams}"
        holder.referees.text = "裁判员: ${tournament.referees}"

        holder.itemView.findViewById<TextView>(R.id.tournamentName).text = tournament.tournamentName
        holder.itemView.setOnClickListener { onTournamentClicked(tournament) }
    }

    override fun getItemCount(): Int = tournamentList.size
}