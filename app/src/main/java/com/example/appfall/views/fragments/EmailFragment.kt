package com.example.appfall.views.fragments

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.example.appfall.R
import com.example.appfall.databinding.FragmentEmailBinding
import com.example.appfall.viewModels.UserViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class EmailFragment: Fragment() {

    private var _binding: FragmentEmailBinding? = null
    private val binding get() = _binding!!
    private val userViewModel: UserViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEmailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Observe LiveData for update responses and errors
        observeUpdateResponse()
        observeUpdateError()

        binding.btnCancel.setOnClickListener {
            findNavController().navigate(R.id.action_emailFragment_to_parametersMainFragment)
        }

        binding.btnConfirm.setOnClickListener {
            val newEmail = binding.editTextEmail.text.toString()
            if (newEmail.isNotEmpty()) {
                userViewModel.updateEmail(newEmail)
            } else {
                binding.emailWarning.visibility = View.VISIBLE
            }
        }

        binding.editTextEmail.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.toString().isNotEmpty()) {
                    binding.emailWarning.visibility = View.GONE
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun observeUpdateResponse() {
        userViewModel.updateEmailResponse.observe(viewLifecycleOwner, Observer { updateResponse ->
            if (updateResponse != null) {
                showPopup("Succès", "L'adresse email a été changé avec succès")
            }
        })

    }

    private fun observeUpdateError() {
        userViewModel.addErrorStatus.observe(viewLifecycleOwner, Observer { errorMessage ->
            if (!errorMessage.isNullOrEmpty()) {
                showPopup("Echec", "Une erreur est survenue lors de la mise à jour de l'adresse email")
            }
        })
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
            findNavController().navigate(R.id.action_emailFragment_to_parametersMainFragment)
        }, 3000)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}