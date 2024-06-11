package com.example.appfall.views.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.appfall.R
import com.example.appfall.viewModels.ParameterViewModel
import com.example.appfall.viewModels.UserViewModel
import com.example.appfall.views.activities.AuthenticationActivity

class ParameterFragment : Fragment() {

    private val userViewModel: UserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Handle the back press
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                findNavController().popBackStack() // Navigate to the previous fragment
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(this, callback)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_parameter, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val changeUsername: LinearLayout = view.findViewById(R.id.layoutChangeName)
        changeUsername.setOnClickListener {
            findNavController().navigate(R.id.action_parameterFragment_to_changeUsernameFragment)
        }

        val layoutDeconnexion: LinearLayout = view.findViewById(R.id.layoutDeconnexion)
        layoutDeconnexion.setOnClickListener {
            println("hellooo")
            logoutUser()
        }

    }

    private fun logoutUser() {
        userViewModel.logout()
        val intent = Intent(requireContext(), AuthenticationActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

}