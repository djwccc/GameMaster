package com.example.gamemaster

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class GroupAdapter(private val groups: Map<String, List<String>>) :
    RecyclerView.Adapter<GroupAdapter.GroupViewHolder>() {

    class GroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val groupName: TextView = itemView.findViewById(R.id.tv_group_name)
        val groupTeams: TextView = itemView.findViewById(R.id.tv_group_teams)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_group, parent, false)
        return GroupViewHolder(view)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        val groupName = groups.keys.elementAt(position)
        val groupTeams = groups[groupName]!!

        holder.groupName.text = groupName
        holder.groupTeams.text = groupTeams.joinToString(", ")
    }

    override fun getItemCount() = groups.size
}