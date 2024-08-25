package com.example.appfall.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.appfall.R

class BarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint()
    private val labelPaint = Paint()
    private var barData: List<List<Float>> = emptyList() // List of data for each bar
    private var barRects: List<List<Rect>> = emptyList() // List of Rects for each bar

    private var currentToast: Toast? = null

    private val barSpacing = 40f // Space between bars
    private val padding = 40f // Space at the start and end of the chart
    private val lineWidth = 8f // Line width

    init {
        paint.isAntiAlias = true
        paint.strokeWidth = lineWidth
        paint.color = Color.BLACK // Color for bars

        labelPaint.isAntiAlias = true
        labelPaint.textSize = 30f // Adjust text size as needed
        labelPaint.color = Color.BLACK // Color of the label text
        labelPaint.textAlign = Paint.Align.CENTER

        // Load custom font
        try {
            val customFont = Typeface.createFromAsset(context.assets, "fonts/myfont.ttf")
            labelPaint.typeface = customFont
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to default font if custom font fails
            labelPaint.typeface = Typeface.DEFAULT
        }
    }

    fun setBarData(data: List<List<Float>>) {
        barData = data
        invalidate() // Redraw the view
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (barData.isEmpty() || width <= 0 || height <= 0) return

        // Adjust bar width and height parameters
        val availableWidth = width - 2 * padding
        val numberOfWeeks = 4 // Total number of weeks for labels
        val barWidth = (availableWidth - (numberOfWeeks - 1) * barSpacing) / numberOfWeeks
        val maxBarHeight = height * 0.5f // Maximum height for bars
        val maxDataValue = barData.flatten().maxOrNull() ?: 1f

        // Use colors from resources
        val colors = listOf(
            ContextCompat.getColor(context, R.color.deadly_depth),
            ContextCompat.getColor(context, R.color.hot_jazz),
            ContextCompat.getColor(context, R.color.light_yellow)
        )

        barRects = List(numberOfWeeks) { index ->
            val weekData = barData.getOrNull(index) ?: emptyList()
            val left = padding + index * (barWidth + barSpacing)
            var top = height.toFloat()
            val rects = mutableListOf<Rect>()

            weekData.forEachIndexed { segmentIndex, value ->
                val segmentHeight = (value / maxDataValue * maxBarHeight).toFloat()
                paint.color = colors.getOrElse(segmentIndex) { Color.GRAY } // Default color if fewer segments

                // Draw each segment
                val rect = Rect(
                    left.toInt(),
                    (top - segmentHeight).toInt(),
                    (left + barWidth).toInt(),
                    top.toInt()
                )
                canvas.drawRect(rect, paint)
                rects.add(rect)

                top -= segmentHeight
            }

            rects
        }

        // Remove lines drawing
        // Draw the bars without lines
        // Draw labels for weeks
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            val x = event.x
            val y = event.y

            // Determine the clicked bar
            val barIndex = barRects.indexOfFirst { rects ->
                rects.any { rect ->
                    rect.contains(x.toInt(), y.toInt())
                }
            }

            if (barIndex >= 0) {
                // Cancel any existing toast
                currentToast?.cancel()

                // Show the toast
                showBarInfoToast(barIndex, x, y)
            }
        }
        return true
    }

    private fun showBarInfoToast(barIndex: Int, x: Float, y: Float) {
        // Create a Toast with custom layout
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as android.view.LayoutInflater
        val toastLayout = inflater.inflate(R.layout.toast_bar_chart_info, null)

        // Get references to the views in the custom layout
        val colorContainer = toastLayout.findViewById<LinearLayout>(R.id.toast_color_container)
        val textViewSauvees = toastLayout.findViewById<TextView>(R.id.toast_label_sauvees)
        val textViewFausses = toastLayout.findViewById<TextView>(R.id.toast_label_fausses)

        // Use colors from resources
        val colors = listOf(
            ContextCompat.getColor(context, R.color.deadly_depth),
            ContextCompat.getColor(context, R.color.hot_jazz),
            ContextCompat.getColor(context, R.color.light_yellow)
        )

        // Get the data for the clicked bar
        val data = barData.getOrNull(barIndex) ?: return

        // Clear previous views
        colorContainer.removeAllViews()

        // Add color squares and labels
        data.forEachIndexed { index, value ->
            val colorSquare = View(context).apply {
                layoutParams = LinearLayout.LayoutParams(30, 30) // Size of the square
                setBackgroundColor(colors.getOrElse(index) { Color.GRAY })
            }
            val textView = TextView(context).apply {
                text = "${getLabelForIndex(index)}: ${value.toInt()}"
                textSize = 16f
                setTextColor(Color.BLACK)
            }

            colorContainer.addView(colorSquare)
            colorContainer.addView(textView)
        }

        // Update specific TextViews for specific data
        if (data.size > 0) {
            textViewSauvees.text = "Sauvées: ${data.getOrNull(0)?.toInt() ?: 0}"
        }
        if (data.size > 1) {
            textViewFausses.text = "Fausses: ${data.getOrNull(1)?.toInt() ?: 0}"
        }

        // Show the custom toast
        currentToast = Toast(context).apply {
            duration = Toast.LENGTH_LONG
            view = toastLayout
            setGravity(Gravity.TOP or Gravity.START, x.toInt(), y.toInt())
            show()
        }
    }

    private fun getLabelForIndex(index: Int): String {
        return when (index) {
            0 -> "Traitées"
            1 -> "Fausses"
            else -> "Unknown"
        }
    }


}
