package com.example.appfall.views.fragments

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.example.appfall.R
import com.example.appfall.viewModels.ParameterViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ChangeUsernameFragment : Fragment() {

    private val parameterViewModel: ParameterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Gestion du bouton retour arrière
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Naviguer vers le fragment précédent
                findNavController().navigate(R.id.action_changeUsernameFragment_to_parameterFragment)
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(this, callback)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_change_username, container, false)

        val usernameEditText: EditText = view.findViewById(R.id.usernameEditText)
        val saveButton: ImageButton = view.findViewById(R.id.saveButton)
        val cancelButton: ImageButton = view.findViewById(R.id.cancelButton)

        saveButton.setOnClickListener {
            val newUsername = usernameEditText.text.toString()
            if (newUsername.isNotEmpty()) {
                // Appel à la fonction pour mettre à jour le nom d'utilisateur
                parameterViewModel.updateSupervisorName(newUsername)
            } else {
                Toast.makeText(requireContext(), "Please enter a valid name", Toast.LENGTH_SHORT).show()
            }
        }

        cancelButton.setOnClickListener {
            findNavController().navigate(R.id.action_changeUsernameFragment_to_parameterFragment)
        }

        parameterViewModel.updateStatus.observe(viewLifecycleOwner, Observer { success ->
            if (success) {
                showPopup("Succès", "Nous avons changé le nom avec succès")
            }
        })

        parameterViewModel.updateErrorStatus.observe(viewLifecycleOwner, Observer { error ->
            if (error.isNotEmpty()) {
                showPopup("Echec", "Nous n'avons pas pu changer le nom")
            }
        })

        return view
    }

    private fun showPopup(title: String, message: String) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.popup_message, null)
        val titleTextView: TextView = dialogView.findViewById(R.id.tvTitle)
        val messageTextView: TextView = dialogView.findViewById(R.id.tvMessage)

        titleTextView.text = title
        messageTextView.text = message

        val alertDialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        alertDialog.show()

        Handler(Looper.getMainLooper()).postDelayed({
            alertDialog.dismiss()
            findNavController().navigate(R.id.action_changeUsernameFragment_to_parameterFragment)
        }, 5000)
    }
}
