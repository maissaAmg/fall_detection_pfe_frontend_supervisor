package com.example.appfall.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.appfall.data.models.UserStats
import com.example.appfall.databinding.ContactStatisticsBinding

class ContactStatisticsAdapter(
    private var contactStatisticsList: List<UserStats> = emptyList() // Default to empty list
) : RecyclerView.Adapter<ContactStatisticsAdapter.ContactStatisticsViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactStatisticsViewHolder {
        val binding = ContactStatisticsBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ContactStatisticsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ContactStatisticsViewHolder, position: Int) {
        val contactStatistics = contactStatisticsList[position]
        holder.bind(contactStatistics)
    }

    override fun getItemCount(): Int = contactStatisticsList.size

    // Method to update data and notify changes
    fun updateData(newContactStatisticsList: List<UserStats>) {
        contactStatisticsList = newContactStatisticsList
        notifyDataSetChanged()
    }

    class ContactStatisticsViewHolder(private val binding: ContactStatisticsBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(contactStatistics: UserStats) {
            // Ensure all fields are properly handled
            binding.userName.text = contactStatistics.user ?: "Unknown User" // Handle potential null
            binding.rescuedFalls.text = "Traitées: ${contactStatistics.rescued}"
            //binding.activeFalls.text = "Active: ${contactStatistics.active}"
            binding.falseFalls.text = "Fausses: ${contactStatistics.`false`}"
        }
    }
}
