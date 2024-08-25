package com.example.appfall.adapters

import android.content.Context
import android.text.SpannableString
import android.text.Spanned
import android.text.style.UnderlineSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.example.appfall.R
import com.example.appfall.data.models.Fall
import com.example.appfall.databinding.FallBinding
import com.example.appfall.views.dialogs.MapDialogFragment

class FallAdapter(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) : RecyclerView.Adapter<FallAdapter.FallViewHolder>() {

    private var fallsList = ArrayList<Fall>()

    fun setFalls(fallsList: List<Fall>) {
        this.fallsList = ArrayList(fallsList)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FallViewHolder {
        val binding = FallBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FallViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return fallsList.size
    }

    override fun onBindViewHolder(holder: FallViewHolder, position: Int) {
        val fall = fallsList[position]
        holder.bind(fall, position + 1)  // Pass position + 1 for sequential numbering
    }

    inner class FallViewHolder(private val binding: FallBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var isExpanded = false

        init {
            binding.root.setOnClickListener {
                isExpanded = !isExpanded
                binding.expandedLayout.visibility = if (isExpanded) View.VISIBLE else View.GONE
                binding.expandIcon.setImageResource(
                    if (isExpanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more
                )
            }

            binding.locationDetails.setOnClickListener {
                val fall = binding.fall ?: return@setOnClickListener
                val fragmentManager = (context as AppCompatActivity).supportFragmentManager
                val mapDialogFragment = MapDialogFragment(fall.place.latitude, fall.place.longitude)
                mapDialogFragment.show(fragmentManager, "MapDialogFragment")
            }
        }

        fun bind(fall: Fall, position: Int) {
            binding.fall = fall // This binds the 'fall' variable to the layout

            val formattedDate = extractDate(fall.dateTime)
            val formattedTime = extractTime(fall.dateTime)

            binding.fallDate.text = formattedDate
            binding.fallTime.text = formattedTime

            binding.position = position
            binding.executePendingBindings()
            binding.expandedLayout.visibility = if (isExpanded) View.VISIBLE else View.GONE
            binding.expandIcon.setImageResource(
                if (isExpanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more
            )

            // Set card background color based on fall status
            val colorRes = when (fall.status) {
                "active" -> R.color.light_red
                "rescued" -> R.color.green
                "false" -> R.color.light_grey
                else -> R.color.white // Define a default color in your colors.xml
            }
            binding.cardView.setCardBackgroundColor(ContextCompat.getColor(binding.root.context, colorRes))

            // Set location details text with underline
            val locationText = "${fall.place.latitude}, ${fall.place.longitude}"
            val spannable = SpannableString(locationText)
            spannable.setSpan(UnderlineSpan(), 0, spannable.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            binding.locationDetails.text = spannable
            binding.locationDetails.setTextColor(ContextCompat.getColor(context, R.color.blue))
        }

        private fun extractDate(dateTime: String): String {
            return dateTime.substringBefore('T')
        }

        private fun extractTime(dateTime: String): String {
            // Extract the time part after 'T' and before 'Z'
            val timePart = dateTime.substringAfter('T').substringBefore('Z')

            // Split the time part by ':' to get hours and minutes
            val timeComponents = timePart.split(':')

            // Check if there are at least two components (hours and minutes)
            if (timeComponents.size >= 2) {
                val hours = timeComponents[0]
                val minutes = timeComponents[1]

                // Format and return only hours and minutes
                return "$hours:$minutes"
            }

            // Return the original timePart if it doesn't meet the expected format
            return timePart
        }
    }
}
