package com.example.appfall.views.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.appfall.R
import com.example.appfall.viewModels.ParameterViewModel

class ChangeUsernameFragment : Fragment() {

    private val parameterViewModel: ParameterViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_change_username, container, false)

        val usernameEditText: EditText = view.findViewById(R.id.usernameEditText)
        val saveButton: ImageButton = view.findViewById(R.id.saveButton)

        saveButton.setOnClickListener {
            val newUsername = usernameEditText.text.toString()
            if (newUsername.isNotEmpty()) {
                // Appel à la fonction pour mettre à jour le nom d'utilisateur
                parameterViewModel.updateSupervisorName(newUsername)
            } else {
                Toast.makeText(requireContext(), "Please enter a valid name", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }
}
