package com.example.myapplication

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.GsonBuilder
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponent
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.places.picker.PlacePicker
import com.mapbox.mapboxsdk.plugins.places.picker.model.PlacePickerOptions
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.*
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncher
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncherOptions
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute
import kotlinx.android.synthetic.main.activity_main.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class MainActivity : AppCompatActivity(), OnMapReadyCallback, PermissionsListener {

    private val ICON_ID = "ICON_ID"
    private var mapbox: MapboxMap? = null
    private var mapView: MapView? = null

    private var permissionManager: PermissionsManager? = null

    private var locationComponent: LocationComponent? = null

    private var currentRoute: DirectionsRoute? = null
    private var TAG = "DirectionsActivity"
    private var navigationMapRoute: NavigationMapRoute? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(this, token)
        setContentView(R.layout.activity_main)

        map.onCreate(savedInstanceState)
        map.getMapAsync(this)
        mapView = map
        //get permission

        fab.setOnClickListener {
            val lastLocation = locationComponent?.lastKnownLocation
            val camera = CameraPosition.Builder()
                .zoom(12.0)
                .padding(1.0, 1.0, 1.0, 1.0)
                .target(
                    LatLng(
                        lastLocation?.latitude ?: 0.0,
                        lastLocation?.longitude ?: 0.0
                    )
                )
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

        fab2.isClickable = false
        fab2.setOnClickListener {
            val simulateRoute = true
            val options = NavigationLauncherOptions.builder()
                .directionsRoute(currentRoute)
                .shouldSimulateRoute(simulateRoute)
                .build()

            NavigationLauncher.startNavigation(this, options)
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
            generateRoot(loc)
        }
    }

    private fun generateRoot(loc: LatLng) {
        val last = locationComponent?.lastKnownLocation
        val origin = Point.fromLngLat(last!!.longitude, last.latitude)
        val destination = Point.fromLngLat(loc.longitude, loc.latitude)

        NavigationRoute.builder(this)
            .accessToken(Mapbox.getAccessToken()!!)
            .origin(origin)
            .destination(destination)
            .build()
            .getRoute(object : Callback<DirectionsResponse> {
                override fun onFailure(call: Call<DirectionsResponse>, t: Throwable) {
                    Log.e(TAG, "Error: " + t.message);
                }

                override fun onResponse(
                    call: Call<DirectionsResponse>,
                    response: Response<DirectionsResponse>
                ) {
                    Log.d(TAG, "Response code: " + response.code())
                    if (response.body() == null) {
                        Log.e(
                            TAG,
                            "No routes found, make sure you set the right user and access token."
                        );
                        return
                    } else if (response.body()!!.routes().size < 1) {
                        Log.e(TAG, "No routes found")
                        return
                    }

                    currentRoute = response.body()!!.routes()[0]
                    //on the map
                    if (navigationMapRoute != null) {
                        navigationMapRoute?.removeRoute()
                    } else {
                        navigationMapRoute = NavigationMapRoute(
                            null, mapView!!,
                            mapbox!!, R.style.NavigationMapRoute
                        )
                    }

                    navigationMapRoute?.addRoute(currentRoute)
                    fab2.isClickable = true
                    mapbox?.moveCamera(CameraUpdateFactory.zoomBy(6.0))

                }
            })
    }


    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapbox = mapboxMap
        mapboxMap.setStyle(Style.MAPBOX_STREETS) { style ->
            enableLocationComponent(style)
            mapbox?.moveCamera(CameraUpdateFactory.zoomBy(12.0))
            addDestinationIconSymbolLayer(style)
        }
    }

    private fun addDestinationIconSymbolLayer(loadedMapStyle: Style) {
        loadedMapStyle.addImage(
            "destination-icon-id",
            BitmapFactory.decodeResource(this.resources, R.drawable.mapbox_marker_icon_default)
        )

        val geoJsonSource = GeoJsonSource("destination-source-id")
        loadedMapStyle.addSource(geoJsonSource)
        val destinationSymbolLayer =
            SymbolLayer("destination-symbol-layer-id", "destination-source-id");
        destinationSymbolLayer.withProperties(
            iconImage("destination-icon-id"),
            iconAllowOverlap(true),
            iconIgnorePlacement(true)
        )
        loadedMapStyle.addLayer(destinationSymbolLayer)
    }

    private fun enableLocationComponent(style: Style) {

        if (PermissionsManager.areLocationPermissionsGranted(this)) {

            val locationComponentActivationOptions = LocationComponentActivationOptions
                .builder(this, style)
                .build()

            locationComponent = mapbox?.locationComponent
            locationComponent?.activateLocationComponent(locationComponentActivationOptions)

            locationComponent?.isLocationComponentEnabled = true
            locationComponent?.cameraMode = CameraMode.TRACKING
            val engine = LocationEngineProvider.getBestLocationEngine(this)
            locationComponent?.locationEngine = engine

        } else {
            permissionManager = PermissionsManager(this)
            permissionManager?.requestLocationPermissions(this)
        }

    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            enableLocationComponent(mapbox?.style!!)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionManager?.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {
        Toast.makeText(this, "Why??", Toast.LENGTH_SHORT).show()
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

    val token =
        "pk.eyJ1IjoiY2l0eWNhcnByZW1pdW0iLCJhIjoiY2sxY3dkOWoxMDF6YjNjcGkzY2h5aXBldCJ9.W5vq2kbSnDz_I5p0m2HZ0A"
}
