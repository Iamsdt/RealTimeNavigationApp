package com.example.myapplication

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.GsonBuilder
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponent
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.LocationComponentOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.places.picker.PlacePicker
import com.mapbox.mapboxsdk.plugins.places.picker.model.PlacePickerOptions
import kotlinx.android.synthetic.main.activity_main.*

@SuppressLint("Registered")
class ExtraCode : AppCompatActivity(), OnMapReadyCallback {

    private val ICON_ID = "ICON_ID"
    private var mapbox: MapboxMap?= null
    private var mapView: MapView?= null

    private var locationComponent: LocationComponent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val token = "pk.eyJ1IjoiY2l0eWNhcnByZW1pdW0iLCJhIjoiY2sxY3dkOWoxMDF6YjNjcGkzY2h5aXBldCJ9.W5vq2kbSnDz_I5p0m2HZ0A"
        Mapbox.getInstance(this, token)
        setContentView(R.layout.activity_main)
        map.onCreate(savedInstanceState)
        map.getMapAsync(this)
        mapView = map
        //get permission

        fab.setOnClickListener {
            val lat = locationComponent?.lastKnownLocation?.latitude
            val lon = locationComponent?.lastKnownLocation?.longitude

            //Toast.makeText(this, "Location: $lat and $lon ", Toast.LENGTH_SHORT).show()

            val camera = CameraPosition.Builder()
                .zoom(6.0)
                .padding(1.0, 1.0, 1.0, 1.0)
                .target(LatLng(40.7544, -73.9862))
                .build()

            val options = PlacePickerOptions.builder()
                .statingCameraPosition(camera)
                .build()

            val intent = PlacePicker.IntentBuilder()
                .accessToken(token)
                .placeOptions(options)
                .build(this)
            startActivityForResult(intent, 121)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == 121) {
            val feature = PlacePicker.getPlace(data)
            val geo = feature?.toJson()
            val json = GsonBuilder().create()
            val plce = json.fromJson<PlaceData>(geo, PlaceData::class.java)
            val list = plce.geometry.coordinates
            val loc = LatLng(list[1], list[0])
            mapbox?.moveCamera(CameraUpdateFactory.newLatLng(loc))
        }
    }


    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapbox = mapboxMap
        mapboxMap.setStyle(Style.MAPBOX_STREETS){ style ->
            enableLocationComponent(style)
        }
        mapbox?.setMinZoomPreference(4.0)
    }

    private fun enableLocationComponent(style: Style) {

        val locationComponentActivationOptions = LocationComponentActivationOptions
            .builder(this, style)
            .build()

        locationComponent = mapbox?.locationComponent
        locationComponent?.activateLocationComponent(locationComponentActivationOptions)

        locationComponent?.isLocationComponentEnabled = true
        locationComponent?.cameraMode = CameraMode.TRACKING_COMPASS

    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        map.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        map.onLowMemory()
    }

    override fun onStart() {
        super.onStart()
        map.onStart()
    }

    override fun onStop() {
        super.onStop()
        map.onStop()
    }
}