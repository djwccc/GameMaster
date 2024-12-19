package com.example.gamemaster

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class GroupMatchAdapter(private var matchList: MutableList<GroupMatchModel>,
                        private val onEditTime: (GroupMatchModel) -> Unit) :
    RecyclerView.Adapter<GroupMatchAdapter.GroupMatchViewHolder>() {

    class GroupMatchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val matchTime: TextView = itemView.findViewById(R.id.tv_match_time)
        val teams: TextView = itemView.findViewById(R.id.tv_teams)
        val referee: TextView = itemView.findViewById(R.id.tv_referee)
        val editTimeButton: Button = itemView.findViewById(R.id.btn_edit_time)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupMatchViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_group_match, parent, false)
        return GroupMatchViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: GroupMatchViewHolder, position: Int) {
        val match = matchList[position]
        holder.matchTime.text = match.matchTime
        holder.teams.text = "${match.teamA} vs ${match.teamB}"
        holder.referee.text = "裁判员: ${match.referee}"

        holder.editTimeButton.setOnClickListener {
            onEditTime(match) // 点击时回调
        }
    }

    override fun getItemCount() = matchList.size

    // 更新数据的方法
    fun updateData(newMatchList: List<GroupMatchModel>) {
        matchList.clear()
        matchList.addAll(newMatchList) // 添加新的数据
        notifyDataSetChanged() // 通知列表刷新
    }
}