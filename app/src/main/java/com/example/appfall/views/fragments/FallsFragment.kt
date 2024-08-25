package com.example.appfall.views.fragments

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appfall.R
import com.example.appfall.adapters.FallAdapter
import com.example.appfall.databinding.FragmentFallsBinding
import com.example.appfall.services.NetworkHelper
import com.example.appfall.viewModels.FallsViewModel
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class FallsFragment : Fragment(), CoroutineScope {

    private lateinit var binding: FragmentFallsBinding
    private lateinit var fallViewModel: FallsViewModel
    private lateinit var fallAdapter: FallAdapter
    private var userId: String = ""
    private var userPhone: String = ""
    private var isPaused: Boolean = false

    private lateinit var errorDialog: AlertDialog

    private var expectedSwitchState: Boolean = false
    private val retryCount = 3
    private val retryDelay = 2000L // Delay in milliseconds

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + Job()

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
            userPhone = it.getString("userPhone", "")
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

        fallAdapter = FallAdapter(requireContext(), viewLifecycleOwner)

        binding.backButton.setOnClickListener {
            findNavController().navigate(R.id.action_fallsFragment_to_contactsFragment)
        }

        binding.fallsList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = fallAdapter
        }

        // Initialize error message visibility
        binding.errorTextViewLayout.visibility = View.GONE

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
                // Update ViewModel
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
            binding.errorTextViewLayout.visibility = View.GONE

            // Start retry mechanism
            retryLoadFalls(type, retryCount)
        } else {
            binding.noConnectionLayout.visibility = View.VISIBLE
            binding.fallsList.visibility = View.GONE
            binding.progressBar.visibility = View.GONE
            binding.errorTextViewLayout.visibility = View.GONE
        }
    }

    private fun retryLoadFalls(type: String, retriesLeft: Int) {
        val job = launch {
            val success = withContext(Dispatchers.IO) {
                try {
                    fallViewModel.getFalls(userId, type)
                    true
                } catch (e: Exception) {
                    Log.e("FallsFragment", "Error loading falls", e)
                    false
                }
            }

            if (success) {
                observeFalls()
            } else if (retriesLeft > 1) {
                // Retry after delay
                delay(retryDelay)
                retryLoadFalls(type, retriesLeft - 1)
            } else {
                // Final failure
                binding.progressBar.visibility = View.GONE
                binding.errorTextViewLayout.visibility = View.VISIBLE
            }
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
            binding.progressBar.visibility = View.GONE
            binding.errorTextViewLayout.visibility = View.GONE
            if (falls.isNullOrEmpty()) {
                binding.emptyListMessage.visibility = View.VISIBLE
                binding.fallsList.visibility = View.GONE
            } else {
                binding.emptyListMessage.visibility = View.GONE
                binding.fallsList.visibility = View.VISIBLE
                fallAdapter.setFalls(ArrayList(falls))
            }
        }
    }

    private fun observePauseStatus() {
        fallViewModel.observePauseStatus().observe(viewLifecycleOwner) { pauseMessage ->
            pauseMessage?.let {
                Log.d("FallsFragment", "Message de pause: $it")
                if (it.contains("successfully", true)) {
                    expectedSwitchState = !expectedSwitchState
                    // Subscribe/Unsubscribe from Firebase topic
                    if (expectedSwitchState) {
                        subscribeToTopic(userPhone)
                    } else {
                        unsubscribeFromTopic(userPhone)
                    }
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
                    unsubscribeFromTopic(userPhone)
                } else {
                    showErrorDialog("échec", "Erreur lors de la déconnexion")
                }
            }
        }
    }

    private fun showDeleteConfirmationDialog(contactId: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_warning, null)
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(dialogView)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // Ajuster la taille de la boîte de dialogue
        val layoutParams = WindowManager.LayoutParams()
        layoutParams.copyFrom(dialog.window?.attributes)
        layoutParams.width = resources.getDimensionPixelSize(R.dimen.dialog_width) // Définissez la largeur souhaitée en dp
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT
        dialog.window?.attributes = layoutParams

        val titleTextView = dialog.findViewById<TextView>(R.id.tvTitle)
        val messageTextView = dialog.findViewById<TextView>(R.id.tvMessage)
        val confirmButton = dialog.findViewById<Button>(R.id.btnConfirm)
        val cancelButton = dialog.findViewById<Button>(R.id.btnCancel)

        titleTextView.text = "Attention!"
        messageTextView.text = "En supprimant cette personne vous ne recevrez plus d'alertes de son appareil."

        confirmButton.setOnClickListener {
            fallViewModel.disconnect(contactId)
            dialog.dismiss()
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

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

    private fun subscribeToTopic(topic: String) {
        FirebaseMessaging.getInstance().subscribeToTopic(topic)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("FallsFragment", "Successfully subscribed to topic: $topic")
                } else {
                    Log.e("FallsFragment", "Failed to subscribe to topic: $topic", task.exception)
                }
            }
    }

    private fun unsubscribeFromTopic(topic: String) {
        FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("FallsFragment", "Successfully unsubscribed from topic: $topic")
                } else {
                    Log.e("FallsFragment", "Failed to unsubscribe from topic: $topic", task.exception)
                }
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineContext.cancel() // Cancel coroutines when fragment is destroyed
    }
}
