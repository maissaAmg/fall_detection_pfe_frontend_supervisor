package com.example.appfall.views.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appfall.R
import com.example.appfall.adapters.ContactStatisticsAdapter
import com.example.appfall.adapters.ContactsAdapter
import com.example.appfall.data.models.DailyFallsResponse
import com.example.appfall.data.models.UserStats
import com.example.appfall.databinding.FragmentProfilBinding
import com.example.appfall.services.NetworkHelper
import com.example.appfall.viewModels.ContactsViewModel
import com.example.appfall.viewModels.FallsViewModel
import com.example.appfall.views.BarChartView
import com.example.appfall.views.activities.ParametersActivity
import java.util.Calendar

class ProfilFragment : Fragment() {

    private lateinit var binding: FragmentProfilBinding
    private lateinit var contactStatisticsAdapter: ContactStatisticsAdapter
    private val fallsViewModel: FallsViewModel by viewModels()
    private lateinit var networkHelper: NetworkHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        networkHelper = NetworkHelper(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProfilBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSpinners()
        setupObservers()

        binding.icSettings.setOnClickListener {
            val intent = Intent(requireContext(), ParametersActivity::class.java)
            startActivity(intent)
        }

        if (networkHelper.isInternetAvailable()) {
            // Show the progress bar while fetching data
            binding.fullPageProgressLayout.visibility = View.VISIBLE
            binding.noNetworkLayout.visibility = View.GONE
            binding.contentLayout.visibility = View.GONE
            binding.noDataTextViewLayout.visibility = View.GONE

            // Fetch data for the selected month and year
            generateAndDisplayChartData()
        } else {
            // Handle no internet connection
            binding.fullPageProgressLayout.visibility = View.GONE
            binding.noNetworkLayout.visibility = View.VISIBLE
            binding.contentLayout.visibility = View.GONE
            binding.noDataTextViewLayout.visibility = View.GONE
        }
    }

    private fun setupRecyclerView() {
        contactStatisticsAdapter = ContactStatisticsAdapter(emptyList())
        binding.usersRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.usersRecyclerView.adapter = contactStatisticsAdapter
    }

    private fun setupSpinners() {
        val months = resources.getStringArray(R.array.months)
        val monthAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, months)
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.monthSpinner.adapter = monthAdapter

        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val years = arrayOf((currentYear - 1).toString(), currentYear.toString(), (currentYear + 1).toString())
        val yearAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, years)
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.yearSpinner.adapter = yearAdapter

        binding.monthSpinner.setSelection(Calendar.getInstance().get(Calendar.MONTH))
        binding.yearSpinner.setSelection(years.indexOf(currentYear.toString()))

        binding.monthSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                generateAndDisplayChartData()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.yearSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                generateAndDisplayChartData()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupObservers() {
        fallsViewModel.dailyFallsData.observe(viewLifecycleOwner) { response ->
            binding.fullPageProgressLayout.visibility = View.GONE

            if (response == null) {
                binding.noDataTextViewLayout.visibility = View.GONE
                binding.contentLayout.visibility = View.GONE
                binding.errorTextViewLayout.visibility = View.VISIBLE
            }
            else if (response.data.isNotEmpty() &&
                response.data.any { it.details.isNotEmpty() || it.users.isNotEmpty() }) {

                // Data available
                binding.noDataTextViewLayout.visibility = View.GONE
                binding.contentLayout.visibility = View.VISIBLE

                updateBarChart(response)
                updateStatisticsUI(response)
                updateRecyclerView(response)

            } else {
                // No data available
                binding.contentLayout.visibility = View.GONE
                binding.noDataTextViewLayout.visibility = View.VISIBLE
            }
        }
    }

    private fun updateBarChart(response: DailyFallsResponse) {
        val details = response.data.firstOrNull()?.details ?: emptyList()

        // Extract data for each week from the response
        val barData = details
            .filter { it.week in 1..4 } // Assuming weeks are between 1 and 52
            .sortedBy { it.week } // Ensure data is sorted by week
            .map { detail ->
                listOf(
                    detail.rescued.toFloat(),
                    detail.`false`.toFloat()
                )
            }

        // Update the BarChartView with the data
        binding.barChartView.post {
            binding.barChartView.setBarData(barData)
            binding.barChartView.visibility = View.VISIBLE
        }
    }

    private fun generateAndDisplayChartData() {
        val selectedMonth = binding.monthSpinner.selectedItemPosition + 1 // January is 1
        val selectedYear = binding.yearSpinner.selectedItem.toString().toInt()

        fallsViewModel.getDailyFalls(selectedMonth, selectedYear)
    }

    private fun updateStatisticsUI(response: DailyFallsResponse) {
        val firstDetail = response.data.firstOrNull()?.details?.firstOrNull()

        binding.totalFalls.text = "Total: ${response.data.firstOrNull()?.total ?: 0}"
        binding.rescuedFalls.text = "Traitées: ${firstDetail?.rescued ?: 0}"
        binding.falseFalls.text = "Fausses: ${firstDetail?.`false` ?: 0}"

        binding.fullPageProgressLayout.visibility = View.GONE
        binding.contentLayout.visibility = View.VISIBLE
    }

    private fun updateRecyclerView(response: DailyFallsResponse) {
        val contactStatisticsList = response.data.flatMap { data ->
            data.users.map { detail ->
                UserStats(
                    active = detail.active,
                    `false` = detail.`false`,
                    rescued = detail.rescued,
                    user = detail.user
                )
            }
        }

        Log.d("ContactStatisticsAdapter", "UserStats: $contactStatisticsList")
        contactStatisticsAdapter.updateData(contactStatisticsList)
    }
}
