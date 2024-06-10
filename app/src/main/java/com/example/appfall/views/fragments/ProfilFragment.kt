package com.example.appfall.views.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ProgressBar
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.appfall.R
import com.example.appfall.data.models.Day
import com.example.appfall.viewModels.FallsViewModel
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.BarData
import java.util.Calendar

class ProfilFragment : Fragment() {
    private lateinit var fallsViewModel: FallsViewModel
    private lateinit var progressBar: ProgressBar
    private lateinit var barChart: BarChart
    private lateinit var monthSpinner: Spinner
    private lateinit var yearSpinner: Spinner

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profil, container, false)
        progressBar = view.findViewById(R.id.progressBar)
        barChart = view.findViewById(R.id.barChart)
        monthSpinner = view.findViewById(R.id.monthSpinner)
        yearSpinner = view.findViewById(R.id.yearSpinner)

        // Initialize month spinner with month names or numbers
        val months = resources.getStringArray(R.array.months)
        val monthAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, months)
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        monthSpinner.adapter = monthAdapter

        // Initialize year spinner with years (you can populate it dynamically)
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val years = arrayOf((currentYear - 1).toString(), currentYear.toString(), (currentYear + 1).toString())
        val yearAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, years)
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        yearSpinner.adapter = yearAdapter

        // Set initial selection to current month and year
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
        monthSpinner.setSelection(currentMonth)

        val currentYearIndex = years.indexOf(currentYear.toString())
        yearSpinner.setSelection(currentYearIndex)

        // Set up listener for month selection
        monthSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // When month selected, update chart data

                    fetchDataAndUpdateChart()

            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }

        // Set up listener for year selection
        yearSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // When year selected, update chart data
                if (view != null) {
                    fetchDataAndUpdateChart()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }

        // Initialize FallsViewModel and observe data
        fallsViewModel = ViewModelProvider(this).get(FallsViewModel::class.java)
        fetchDataAndUpdateChart()

        return view
    }


    private fun fetchDataAndUpdateChart() {
        val selectedMonth = monthSpinner.selectedItemPosition
        val selectedYear = yearSpinner.selectedItem.toString().toInt()
        println("Selected year ${selectedYear}")
        println("Selected month ${selectedMonth}")
        fallsViewModel.getDailyFalls("662043ca50a2db0cdd6ecba5", selectedMonth + 1, selectedYear)
        observeDailyFalls()

        monthSpinner.visibility = View.VISIBLE
        yearSpinner.visibility = View.VISIBLE
    }


    private fun observeDailyFalls() {
        fallsViewModel.observeDailyFalls().observe(viewLifecycleOwner) { falls ->
            falls?.let {
                setupBarChart(falls.data)
                progressBar.visibility = View.GONE
                barChart.visibility = View.VISIBLE
            }
        }
    }

    private fun setupBarChart(falls: List<Day>) {
        val rescuedEntries = ArrayList<BarEntry>()
        val activeEntries = ArrayList<BarEntry>()
        val falseEntries = ArrayList<BarEntry>()

        // Assuming the weeks are from 1 to 5 for a month
        val weeksInMonth = 5

        // Initialize entries with 0 for all weeks
        for (week in 1..weeksInMonth) {
            rescuedEntries.add(BarEntry(week.toFloat(), 0f))
            activeEntries.add(BarEntry(week.toFloat(), 0f))
            falseEntries.add(BarEntry(week.toFloat(), 0f))
        }

        // Populate entries with actual data
        for (dayData in falls) {
            val week = dayData.week
            // Check if week is not null and has the expected format
            if (week.isNotEmpty() && week.length >= 8) {
                // Extract the total week number from the "week" field
                val totalWeekNumber = week.substring(6, 8).toInt() // "2024-W22" -> 22
                // Calculate the week within the month using Euclidean division
                val weekWithinMonth = (totalWeekNumber - 1) % weeksInMonth + 1

                for (count in dayData.counts) {
                    when (count.status) {
                        "rescued" -> rescuedEntries[weekWithinMonth - 1].y = count.count.toFloat()
                        "active" -> activeEntries[weekWithinMonth - 1].y = count.count.toFloat()
                        "false" -> falseEntries[weekWithinMonth - 1].y = count.count.toFloat()
                    }
                }
            } else {
                // Handle the case where the week field is null, empty, or doesn't have the expected format
            }
        }


        val colorRescued = resources.getColor(R.color.custom_red, null)
        val colorActive = resources.getColor(R.color.deep_blue, null)
        val colorFalse = resources.getColor(R.color.light_yellow, null)

        val barDataSetRescued = BarDataSet(rescuedEntries, "Rescued")
        barDataSetRescued.color = colorRescued

        val barDataSetActive = BarDataSet(activeEntries, "Active")
        barDataSetActive.color = colorActive

        val barDataSetFalse = BarDataSet(falseEntries, "False")
        barDataSetFalse.color = colorFalse

        val data = BarData(barDataSetRescued, barDataSetActive, barDataSetFalse)
        barChart.data = data

        barChart.description.isEnabled = false
        barChart.setDrawGridBackground(false)

        val xAxis: XAxis = barChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(true) // Enable X-axis gridlines
        xAxis.granularity = 1f
        xAxis.textSize = 12f // Set the X-axis label text size

        val leftAxis: YAxis = barChart.axisLeft
        leftAxis.setDrawGridLines(true) // Enable left Y-axis gridlines
        leftAxis.textSize = 12f // Set the left Y-axis label text size
        val rightAxis: YAxis = barChart.axisRight
        rightAxis.setDrawGridLines(true) // Enable right Y-axis gridlines
        rightAxis.textSize = 12f // Set the right Y-axis label text size

        val legend = barChart.legend
        legend.textSize = 14f // Set the legend text size

        barDataSetRescued.valueTextSize = 10f // Set the value text size for rescued data set
        barDataSetActive.valueTextSize = 10f // Set the value text size for active data set
        barDataSetFalse.valueTextSize = 10f // Set the value text size for false data set

        // Disable zooming and panning
        barChart.setScaleEnabled(false)
        barChart.setPinchZoom(false)
        barChart.isDoubleTapToZoomEnabled = false

        // Add animations
        barChart.animateXY(1000, 1000)

        barChart.invalidate()
    }
}
