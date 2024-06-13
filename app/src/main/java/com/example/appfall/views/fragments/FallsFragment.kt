package com.example.appfall.views.fragments

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.fragment.app.Fragment
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
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
import com.example.appfall.services.NetworkHelper
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

        loadFalls("all")

        binding.btnAll.setOnClickListener {
            loadFalls("all")
            setButtonState(binding.btnAll) { observeFalls() }
        }

        binding.btnActive.setOnClickListener {
            loadFalls("active")
            setButtonState(binding.btnActive) { observeFalls() }
        }

        binding.btnRescued.setOnClickListener {
            loadFalls("rescued")
            setButtonState(binding.btnRescued) { observeFalls() }
        }

        binding.btnFalse.setOnClickListener {
            loadFalls("false")
            setButtonState(binding.btnFalse) { observeFalls() }
        }

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

        observeFalls()
        observePauseStatus()
        observeDisconnectStatus()
    }

    private fun loadFalls(type: String) {
        val networkHelper = NetworkHelper(requireContext())
        if (networkHelper.isInternetAvailable()) {
            binding.noConnectionLayout.visibility = View.GONE
            binding.fallsList.visibility = View.GONE
            binding.progressBar.visibility = View.VISIBLE
            fallViewModel.getFalls(userId, type)
            observeFalls()
        } else {
            binding.noConnectionLayout.visibility = View.VISIBLE
            binding.fallsList.visibility = View.GONE
            binding.progressBar.visibility = View.GONE
        }
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
                binding.progressBar.visibility = View.GONE
                binding.fallsList.visibility = View.VISIBLE
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
                    showErrorDialog("échec", "Erreur lors du changement de l'état du suivi")
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
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_error)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // Ajuster la taille de la boîte de dialogue
        val layoutParams = WindowManager.LayoutParams()
        layoutParams.copyFrom(dialog.window?.attributes)
        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT
        dialog.window?.attributes = layoutParams

        val titleTextView = dialog.findViewById<TextView>(R.id.tvTitle)
        val messageTextView = dialog.findViewById<TextView>(R.id.tvMessage)
        val okButton = dialog.findViewById<Button>(R.id.btnOk)

        titleTextView.text = title
        messageTextView.text = message

        okButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}

