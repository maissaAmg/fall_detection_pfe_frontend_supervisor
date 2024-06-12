package com.example.appfall.views.fragments

import androidx.fragment.app.Fragment
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
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

    private lateinit var errorDialog: AlertDialog

    private var expectedSwitchState: Boolean = false

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
            userId = it.getString("userId", "")
            isPaused = it.getBoolean("isPaused", false)
        }

        // Initialize the expectedSwitchState based on isPaused
        expectedSwitchState = !isPaused
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

        Log.d("FallsFragmentUserId", "$userId")
        Log.d("FallsFragmentUserId", "*********************************")

        fallViewModel.getFalls(userId, "all")

        binding.btnAll.setOnClickListener {
            fallViewModel.getFalls(userId, "all")
            setButtonState(binding.btnAll) { observeFalls() }
        }

        binding.btnActive.setOnClickListener {
            fallViewModel.getFalls(userId, "active")
            setButtonState(binding.btnActive) { observeFalls() }
        }

        binding.btnRescued.setOnClickListener {
            fallViewModel.getFalls(userId, "rescued")
            setButtonState(binding.btnRescued) { observeFalls() }
        }

        binding.btnFalse.setOnClickListener {
            fallViewModel.getFalls(userId, "false")
            setButtonState(binding.btnFalse) { observeFalls() }
        }

        observeFalls()

        binding.btnDelete.setOnClickListener {
            showDeleteConfirmationDialog(userId)
        }

        // Initial state for Switch and TextView
        binding.switchPauseTracking.isChecked = expectedSwitchState
        updateStatusText(expectedSwitchState)

        // Switch listener
        binding.switchPauseTracking.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked != expectedSwitchState) {
                fallViewModel.pause(userId, !isChecked)
            }
        }

        observePauseStatus()
        observeDisconnectStatus()
    }

    private fun setButtonState(clickedButton: Button, observerFunction: () -> Unit) {
        val buttons = listOf(binding.btnAll, binding.btnActive, binding.btnRescued, binding.btnFalse)
        for (button in buttons) {
            button.setBackgroundResource(R.drawable.rounded_button_filter_empty)
            button.setTextColor(button.context.getColor(R.color.black))
        }

        clickedButton.setBackgroundResource(R.drawable.rounded_button_filter)
        clickedButton.setTextColor(clickedButton.context.getColor(R.color.white))

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
                if (it.contains("successfully", true)) {
                    expectedSwitchState = !expectedSwitchState
                    binding.switchPauseTracking.setOnCheckedChangeListener(null)
                    binding.switchPauseTracking.isChecked = expectedSwitchState
                    binding.switchPauseTracking.setOnCheckedChangeListener { _, isChecked ->
                        if (isChecked != expectedSwitchState) {
                            fallViewModel.pause(userId, !isChecked)
                        }
                    }
                    updateStatusText(expectedSwitchState)
                } else {
                    showErrorDialog("échec", "Erreur lors du changement de l'état de suivi")
                    binding.switchPauseTracking.setOnCheckedChangeListener(null)
                    binding.switchPauseTracking.isChecked = expectedSwitchState
                    binding.switchPauseTracking.setOnCheckedChangeListener { _, isChecked ->
                        if (isChecked != expectedSwitchState) {
                            fallViewModel.pause(userId, !isChecked)
                        }
                    }
                }
            }
        }
    }

    private fun observeDisconnectStatus() {
        fallViewModel.observeDisconnectStatus().observe(viewLifecycleOwner) { disconnectMessage ->
            disconnectMessage?.let {
                Log.d("FallsFragment", "Message de déconnexion: $it")
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
        builder.setPositiveButton("Confirmer") { dialog, _ ->
            fallViewModel.disconnect(contactId)
        }
        builder.setNegativeButton("Annuler") { dialog, _ ->
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

    private fun showErrorDialog(title: String, message: String) {
        val builder = AlertDialog.Builder(requireContext())
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.popup_message, null)
        builder.setView(view)
        val titleTextView = view.findViewById<TextView>(R.id.tvTitle)
        val messageTextView = view.findViewById<TextView>(R.id.tvMessage)
        titleTextView.text = title
        messageTextView.text = message
        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
        }
        errorDialog = builder.create()
        errorDialog.show()
    }
}
