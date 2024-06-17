package com.example.appfall.views.dialogs

import android.app.Dialog
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Window
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.appfall.R
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.symbolLayer
import com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource

class MapDialogFragment(private val latitude: Double, private val longitude: Double) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_map, null)
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(dialogView)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // Adjust dialog size
        val layoutParams = WindowManager.LayoutParams()
        layoutParams.copyFrom(dialog.window?.attributes)
        layoutParams.width = resources.getDimensionPixelSize(R.dimen.dialog_width) // Set the desired width in dp
        layoutParams.height = resources.getDimensionPixelSize(R.dimen.dialog_height)
        dialog.window?.attributes = layoutParams

        val mapView = dialog.findViewById<MapView>(R.id.dialogMapView)
        mapView.getMapboxMap().loadStyleUri("mapbox://styles/mapbox/streets-v11") { style ->
            style.addImage(
                "marker-icon-id",
                BitmapFactory.decodeResource(resources, R.drawable.localisation_icon)
            )
            val point = Point.fromLngLat(longitude, latitude)
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

        lifecycle.addObserver(object : DefaultLifecycleObserver {
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

        return dialog
    }
}
