package com.example.appfall.adapters

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.appfall.R
import com.example.appfall.databinding.ContactBinding
import com.example.appfall.data.models.ConnectedSupervisor

class ContactsAdapter ( private val onContactClick: (String, String, Boolean) -> Unit) : RecyclerView.Adapter<ContactsAdapter.ContactsViewHolder>() {
    private var contactsList = ArrayList<ConnectedSupervisor>()

    fun setContacts(contactsList: ArrayList<ConnectedSupervisor>) {
        this.contactsList = contactsList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactsViewHolder {
        val binding = ContactBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ContactsViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return contactsList.size
    }

    override fun onBindViewHolder(holder: ContactsViewHolder, position: Int) {
        val contact = contactsList[position]
        holder.bind(contact)
    }

    inner class ContactsViewHolder(private val binding: ContactBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(contact: ConnectedSupervisor) {
            binding.apply {
                /*Glide.with(holder.itemView)
                    .load(contact.urlImage)
                    .into(contactImage)*/
                contactName.text = contact.name
                val cardColor = when {
                    contact.isPaused -> R.color.light_yellow
                    contact.inDanger -> R.color.light_red
                    else -> R.color.white
                }

                println("isPaused ${contact.isPaused}")
                println("inDanger ${contact.inDanger}")
                root.setCardBackgroundColor(ContextCompat.getColor(root.context, cardColor))

                root.setOnClickListener { onContactClick(contact._id, contact.phone, contact.isPaused) }
            }
        }
    }
}