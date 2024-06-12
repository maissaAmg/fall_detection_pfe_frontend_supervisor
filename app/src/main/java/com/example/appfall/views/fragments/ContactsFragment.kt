package com.example.appfall.views.fragments

import android.content.Intent
import com.example.appfall.viewModels.ContactsViewModel
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
import com.example.appfall.views.activities.ParametersActivity


class ContactsFragment : Fragment() {

    private lateinit var binding: FragmentContactsBinding
    private lateinit var contactsViewModel: ContactsViewModel
    private lateinit var contactsAdapter: ContactsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        contactsViewModel = ViewModelProvider(this)[ContactsViewModel::class.java]
        contactsAdapter = ContactsAdapter { contactId, isPaused ->
            val action = ContactsFragmentDirections.actionContactsFragmentToFallsFragment(contactId, isPaused)
            findNavController().navigate(action)
        }
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

        contactsViewModel.getContacts()
        observeContacts()
        observeLoading()
        observeEmptyList()

        // Ajoutez le gestionnaire de clic pour l'icône de paramètres
        binding.icSettings.setOnClickListener {
            val intent = Intent(requireContext(), ParametersActivity::class.java)
            startActivity(intent)
        }
    }

    private fun observeContacts() {
        contactsViewModel.observeContactsList().observe(viewLifecycleOwner) { contacts ->
            println("contacts ${contacts}")
            contacts?.let {
                contactsAdapter.setContacts(ArrayList(it))
            }
        }
    }

    private fun observeLoading() {
        contactsViewModel.observeLoading().observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.contactsList.visibility = if (isLoading) View.GONE else View.VISIBLE
        }
    }

    private fun observeEmptyList() {
        contactsViewModel.observeIsListEmpty().observe(viewLifecycleOwner) { isEmpty ->
            binding.noContactsText.visibility = if (isEmpty) View.VISIBLE else View.GONE
        }
    }
}

