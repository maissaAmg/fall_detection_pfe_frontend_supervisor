package com.example.appfall.views.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appfall.R
import com.example.appfall.adapters.ContactsAdapter
import com.example.appfall.databinding.FragmentContactsBinding
import com.example.appfall.services.NetworkHelper
import com.example.appfall.viewModels.ContactsViewModel
import com.example.appfall.views.activities.ParametersActivity

class ContactsFragment : Fragment() {

    private lateinit var binding: FragmentContactsBinding
    private lateinit var contactsViewModel: ContactsViewModel
    private lateinit var contactsAdapter: ContactsAdapter
    private lateinit var networkHelper: NetworkHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        contactsViewModel = ViewModelProvider(this)[ContactsViewModel::class.java]
        contactsAdapter = ContactsAdapter { contactId, contactPhone, isPaused ->
            val action = ContactsFragmentDirections.actionContactsFragmentToFallsFragment(contactId,  isPaused, contactPhone)
            findNavController().navigate(action)
        }
        networkHelper = NetworkHelper(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentContactsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.contactsList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = contactsAdapter
        }

        binding.icSettings.setOnClickListener {
            val intent = Intent(requireContext(), ParametersActivity::class.java)
            startActivity(intent)
        }

        if (networkHelper.isInternetAvailable()) {
            binding.progressBar.visibility = View.VISIBLE
            contactsViewModel.getContacts()
            observeContacts()
        } else {
            binding.icNoWifi.visibility = View.VISIBLE
            binding.noWifiText.visibility = View.VISIBLE
            binding.progressBar.visibility = View.GONE
            binding.contactsList.visibility = View.GONE
        }
    }

    private fun observeContacts() {
        contactsViewModel.observeContactsList().observe(viewLifecycleOwner) { contacts ->
            binding.progressBar.visibility = View.GONE
            if (contacts.isNullOrEmpty()) {
                binding.noContactsText.visibility = View.VISIBLE
                binding.contactsList.visibility = View.GONE
            } else {
                binding.noContactsText.visibility = View.GONE
                binding.contactsList.visibility = View.VISIBLE
                contactsAdapter.setContacts(ArrayList(contacts))
            }
        }
    }
}
