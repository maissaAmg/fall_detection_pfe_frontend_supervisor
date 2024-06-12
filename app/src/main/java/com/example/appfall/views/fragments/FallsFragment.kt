package com.example.appfall.views.fragments

import androidx.fragment.app.Fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appfall.R
import com.example.appfall.adapters.FallAdapter
import com.example.appfall.databinding.FragmentFallsBinding
import com.example.appfall.viewModels.FallsViewModel

class FallsFragment : Fragment() {

    private lateinit var binding: FragmentFallsBinding
    private lateinit var fallViewModel: FallsViewModel
    private lateinit var fallAdapter: FallAdapter
    private var userId: String = ""
    private var isPaused: Boolean = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fallViewModel = ViewModelProvider(this)[FallsViewModel::class.java]

        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                findNavController().navigate(R.id.action_fallsFragment_to_contactsFragment)
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(this, callback)

        arguments?.let {
            userId = it.getString("userId","")
            isPaused = it.getBoolean("isPaused", false)
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFallsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fallAdapter = FallAdapter(viewLifecycleOwner)

        binding.fallsList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = fallAdapter
        }


        //val userId = "662043ca50a2db0cdd6ecba5"
        //val userId = arguments?.getString("userId") ?: return
        //val id = arguments?.getString("userId") ?: return
        Log.d("FallsFragmentUserId","$userId")
        Log.d("FallsFragmentUserId","*********************************")

        // Ajouter cet appel pour charger toutes les chutes par défaut
        fallViewModel.getFalls(userId, "all")

        binding.btnAll.setOnClickListener {
            fallViewModel.getFalls(userId,"all")
            setButtonState(binding.btnAll) { observeFalls() }
        }

        binding.btnActive.setOnClickListener {
            fallViewModel.getFalls(userId,"active")
            setButtonState(binding.btnActive) { observeFalls() }
        }

        binding.btnRescued.setOnClickListener {
            fallViewModel.getFalls(userId,"rescued")
            setButtonState(binding.btnRescued) { observeFalls() }
        }

        binding.btnFalse.setOnClickListener {
            fallViewModel.getFalls(userId,"false")
            setButtonState(binding.btnFalse) { observeFalls() }
        }


        observeFalls()


        binding.btnDelete.setOnClickListener {
            showDeleteConfirmationDialog(userId)

        }

        // Initial state for Switch and TextView
        binding.switchPauseTracking.isChecked = !isPaused
        updateStatusText(!isPaused)


        // Switch listener
        binding.switchPauseTracking.setOnCheckedChangeListener { _, isChecked ->
            fallViewModel.pause(userId, !isChecked)
            updateStatusText(isChecked)
        }



        observePauseStatus()
        observeDisconnectStatus()
    }

    private fun setButtonState(clickedButton: Button, observerFunction: () -> Unit) {
        // Reset background and text color for all buttons
        val buttons = listOf(binding.btnAll, binding.btnActive, binding.btnRescued, binding.btnFalse)
        for (button in buttons) {
            button.setBackgroundResource(R.drawable.rounded_button_filter_empty)
            button.setTextColor(button.context.getColor(R.color.black))
        }

        // Set background and text color for the clicked button
        clickedButton.setBackgroundResource(R.drawable.rounded_button_filter)
        clickedButton.setTextColor(clickedButton.context.getColor(R.color.white))

        // Call the observer function associated with the clicked button
        observerFunction()
    }

    private fun observeFalls() {
        fallViewModel.observeFallsList().observe(viewLifecycleOwner) { falls ->
            falls?.let {
                fallAdapter.setFalls(ArrayList(it))
            }
        }
    }

    private fun observePauseStatus() {
        fallViewModel.observePauseStatus().observe(viewLifecycleOwner) { pauseMessage ->
            pauseMessage?.let {
                Log.d("FallsFragment", "Message de pause: $it")
                // Affichez un message à l'utilisateur si nécessaire

            }
        }
    }

    private fun observeDisconnectStatus() {
        fallViewModel.observeDisconnectStatus().observe(viewLifecycleOwner) { disconnectMessage ->
            disconnectMessage?.let {
                Log.d("FallsFragment", "Message de déconnexion: $it")
                // Affichez un message à l'utilisateur si
                if (it == "Disconnected successfully") {
                    findNavController().navigate(R.id.action_fallsFragment_to_contactsFragment)
                }

            }
        }
    }

    private fun showDeleteConfirmationDialog(contactId: String) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Alerte !")
        builder.setMessage("En supprimant cette personne, vous ne recevrez plus d'alertes de son appareil.")
        builder.setPositiveButton("Confirmer") { dialog, which ->
            fallViewModel.disconnect(contactId)  // Appeler la méthode de déconnexion
        }
        builder.setNegativeButton("Annuler") { dialog, which ->
            dialog.dismiss()
        }

        val dialog = builder.create()
        dialog.show()
    }

    private fun updateStatusText(isChecked: Boolean) {
        if (isChecked) {
            binding.switchStatus.text = "Activé"
            binding.switchStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.custom_red))
        } else {
            binding.switchStatus.text = "En pause"
            binding.switchStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey))
        }
    }


}

