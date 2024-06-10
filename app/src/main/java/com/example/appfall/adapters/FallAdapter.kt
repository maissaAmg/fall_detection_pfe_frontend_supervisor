package com.example.appfall.adapters

import android.graphics.BitmapFactory
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.example.appfall.R
import com.example.appfall.data.models.Fall
import com.example.appfall.databinding.FallBinding
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.symbolLayer
import com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource



class FallAdapter(private val lifecycleOwner: LifecycleOwner) : RecyclerView.Adapter<FallAdapter.FallViewHolder>() {
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

            // Initialize Mapbox MapView with fall's location
            val mapView = binding.mapView

            lifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    mapView.onStart()
                }

                override fun onStop(owner: LifecycleOwner) {
                    mapView.onStop()
                }
                override fun onDestroy(owner: LifecycleOwner) {
                    mapView.onDestroy()
                }
            })

            mapView.getMapboxMap().loadStyleUri("mapbox://styles/mapbox/streets-v11") { style ->
                // Add the marker icon image to the style
                style.addImage(
                    "marker-icon-id", // This id should match the one used in the symbolLayer
                    BitmapFactory.decodeResource(binding.root.context.resources, R.drawable.localisation_icon)
                )

                val point = Point.fromLngLat(fall.place.longitude, fall.place.latitude)
                style.addSource(geoJsonSource("source-id") {
                    featureCollection(FeatureCollection.fromFeature(Feature.fromGeometry(point)))
                })
                style.addLayer(symbolLayer("layer-id", "source-id") {
                    iconImage("marker-icon-id")
                    iconAnchor(IconAnchor.BOTTOM)
                    iconAllowOverlap(true)
                    iconOffset(listOf(0.0, -9.0))
                    iconSize(0.03)
                    iconOpacity(1.0)
                    visibility(Visibility.VISIBLE)
                })
                mapView.getMapboxMap().setCamera(
                    CameraOptions.Builder()
                        .center(point)
                        .zoom(12.0)
                        .build()
                )
            }
        }

        private fun extractDate(dateTime: String): String {
            return dateTime.substringBefore('T')
        }

        private fun extractTime(dateTime: String): String {
            return dateTime.substringAfter('T').substringBefore('Z')
        }
    }
}
