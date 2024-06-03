package com.example.appfall.views.fragments

import androidx.fragment.app.Fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appfall.R
import com.example.appfall.adapters.FallAdapter
import com.example.appfall.databinding.FragmentFallsBinding
import com.example.appfall.viewModels.FallsViewModel

class FallsFragment : Fragment() {

    private lateinit var binding: FragmentFallsBinding
    private lateinit var fallViewModel: FallsViewModel
    private lateinit var fallAdapter: FallAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fallViewModel = ViewModelProvider(this)[FallsViewModel::class.java]
        fallAdapter = FallAdapter()
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



        binding.fallsList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = fallAdapter
        }


        val userId = "662043ca50a2db0cdd6ecba5"
        //val userId = arguments?.getString("userId") ?: return
        val id = arguments?.getString("userId") ?: return
        Log.d("FallsFragmentUserId","$id")
        Log.d("FallsFragmentUserId","*********************************")


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
            fallViewModel.getFalls(userId,"rescued")
            setButtonState(binding.btnFalse) { observeFalls() }
        }


        observeFalls()
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
}

