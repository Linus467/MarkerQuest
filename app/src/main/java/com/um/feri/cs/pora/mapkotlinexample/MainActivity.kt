package com.um.feri.cs.pora.mapkotlinexample

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.location.GnssAntennaInfo.Listener
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.MotionEvent
import android.text.method.Touch
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.helper.widget.Layer
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.forEach
import org.json.JSONArray
import org.json.JSONObject
import org.osmdroid.config.Configuration.*
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapController
import org.osmdroid.views.MapView
import org.osmdroid.views.Projection
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import org.osmdroid.views.overlay.mylocation.SimpleLocationOverlay
import java.io.File
import java.util.*


class MainActivity : AppCompatActivity(),LocationListener {
    private val REQUEST_PERMISSIONS_REQUEST_CODE = 1
    private lateinit var map : MapView
    private var previousMarker: Marker? = null;
    private var userLocationRightNow: GeoPoint? = null
    private var pathLine: Polyline? = null
    private lateinit var locationManager: LocationManager
    var startpoint : GeoPoint = GeoPoint(50.98369865472108, 7.1198313230549255)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))

        //inflate and create the map
        setContentView(R.layout.activity_main)

        //Creating the Map
        map = findViewById<MapView>(R.id.map)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)

        //Setting default zoom and location values to go to the BIB as standard
        val mapController = map.controller
        mapController.setZoom(15.0)
        mapController.setCenter(startpoint)

        //Getting last location to be displayed on the Map
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            //Get last known location
            val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if (location != null) {
                val currentLocation = GeoPoint(location.latitude, location.longitude)
                addUserMarker(currentLocation)
            } else {
                mapController.setCenter(startpoint)
                mapController.setZoom(15.0)
            }
        }
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
        } else {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000L,
                10f,
                this
            )
        }


        //Settings for the Map
        val mRotationGestureOverlay = RotationGestureOverlay(map)
        mRotationGestureOverlay.isEnabled = true
        map.setMultiTouchControls(true)
        map.overlays.add(mRotationGestureOverlay)

        //Adding markers from JSON file
        try{
            val markerList = loadMarkersFromSharedPreferences(this)
            markerList.forEach { marker ->
                addSavedMarker(marker)
            }
        }finally {

        }

        // Get the preferences instance for this activity
        val preferences = getPreferences(Context.MODE_PRIVATE)

        // Write a string preference
        preferences.edit().putString("example_key", "example_value").apply()

        // Read the string preference
        val value = preferences.getString("example_key", null)

    }
    //Adding Saved Marker to the App on Start
    private fun addSavedMarker(marker : Marker){
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker.setOnMarkerClickListener { marker, mapView ->
            marker.showInfoWindow()
            mapView.controller.animateTo(marker.position)
            val fromLocation: GeoPoint = userLocationRightNow!!
            val toLocation: GeoPoint = marker.position
            val pathPoints: MutableList<GeoPoint> = mutableListOf(
                fromLocation, toLocation
            )
            pathLine?.let { map.overlays.remove(it) } // remove existing path
            pathLine = Polyline().apply {
                setPoints(pathPoints)
                color = Color.BLUE
                width = 5f
            }
            map.overlays.add(pathLine)
            true
        }
        map.overlays.add(marker)
    }
    private fun addMarker(location: GeoPoint) {
        val marker = Marker(map)
        var position = location
        marker.position = position
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        val alertDialogBuilder = AlertDialog.Builder(this@MainActivity)
        val input = EditText(this@MainActivity)
        alertDialogBuilder.setView(input)
        alertDialogBuilder.setTitle("Enter Marker Title")
        alertDialogBuilder.setPositiveButton("OK") { dialog, _ ->
            marker.title = input.text.toString()
            marker.setOnMarkerClickListener { marker, mapView ->
                marker.showInfoWindow()
                mapView.controller.animateTo(marker.position)
                val fromLocation: GeoPoint = userLocationRightNow!!
                val toLocation: GeoPoint = position!!
                val pathPoints: MutableList<GeoPoint> = mutableListOf(
                    fromLocation, toLocation
                )
                pathLine?.let { map.overlays.remove(it) } // remove existing path
                pathLine = Polyline().apply {
                    setPoints(pathPoints)
                    color = Color.BLUE
                    width = 5f
                }
                map.overlays.add(pathLine)
                true
            }
            map.overlays.add(marker)
        }
        alertDialogBuilder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }
        alertDialogBuilder.show()
    }


    private fun addUserMarker(location: GeoPoint) {
        // Remove the previous marker if it exists
        previousMarker?.let {
            map.overlays.remove(it)
        }
        userLocationRightNow = location
        // Add a new marker at the given location
        val marker = Marker(map)
        marker.position = location
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker.icon = ContextCompat.getDrawable(this, R.drawable.baseline_expand_less_24)
        marker.title = "Meine Position"
        map.overlays.add(marker)
        // Store the new marker as the previous marker
        previousMarker = marker

        // Refresh the map to update the marker
        map.invalidate()
    }

    override fun onLocationChanged(location: Location) {
        val currentLocation = GeoPoint(location.latitude, location.longitude)

        val mapController = map.controller

        //Setting User
        val userIcon = ContextCompat.getDrawable(this, R.drawable.baseline_expand_less_24)
        addUserMarker(currentLocation)
        if(pathLine!= null){
            pathLine!!.actualPoints[0].latitude = currentLocation.latitude
            pathLine!!.actualPoints[0].longitude = currentLocation.longitude
            pathLine!!.actualPoints[0].altitude = currentLocation.altitude
        }

    }
    override fun onResume() {
        super.onResume()
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        map.onResume() //needed for compass, my location overlays, v6.0.0 and up
    }

    override fun onPause() {
        super.onPause()
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().save(this, prefs);
        map.onPause()  //needed for compass, my location overlays, v6.0.0 and up
    }

    override fun onDestroy() {
        var markerList : MutableList<Marker> = mutableListOf()
        map.overlays.forEach{ Layer ->
            if(Layer is Marker){
                val l1 : Marker = Layer as Marker
                if(l1.title != "Meine Position"){
                    markerList!!.add(Layer as Marker)
                }
            }
        }
        saveMarkersToSharedPreferences(this,markerList)
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val permissionsToRequest = ArrayList<String>()
        var i = 0
        while (i < grantResults.size) {
            permissionsToRequest.add(permissions[i])
            i++
        }
        if (permissionsToRequest.size > 0) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                REQUEST_PERMISSIONS_REQUEST_CODE)
        }
    }

    fun onClickDraw1(view: View) {
        addMarker(map.getMapCenter() as GeoPoint)
        var markerList : MutableList<Marker> = mutableListOf()
        map.overlays.forEach{ Layer ->
            if(Layer is Marker){
                val l1 : Marker = Layer as Marker
                if(l1.title != "Meine Position"){
                    markerList!!.add(Layer as Marker)
                }
            }
        }
        saveMarkersToSharedPreferences(this,markerList)
    }
    fun onClickDraw3(view: View?) {
        val mapNorthCompassOverlay = object: CompassOverlay(this, map) {
            override fun draw(c: Canvas?, pProjection: Projection?) {
                drawCompass(c, -map.mapOrientation, pProjection?.screenRect)
            }
        }

        map.overlays.add(mapNorthCompassOverlay)
    }

    fun onClickDraw4(view: View?) {
        var controller = map.controller
        controller.setCenter(userLocationRightNow)
    }



    /*// Save markers to a JSON file
    fun saveMarkersToJsonFile(context: Context, markers: List<Marker>) {
        val jsonArray = JSONArray()
        markers.forEach { marker ->
            val jsonObject = JSONObject()
            jsonObject.put("latitude", marker.position.latitude)
            jsonObject.put("longitude", marker.position.longitude)
            jsonObject.put("title", marker.title)
            jsonArray.put(jsonObject)
        }
        val jsonString = jsonArray.toString()

        val fileName = "markers.json"
        val file = File(context.filesDir, fileName)
        file.writeText(jsonString)
    }

    // Load markers from a JSON file
    fun loadMarkersFromJsonFile(context: Context): List<Marker> {
        val fileName = "markers.json"
        val file = File(context.filesDir, fileName)

        if (!file.exists()) {
            return emptyList()
        }

        val jsonString = try {
            file.readText()
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }

        val markers = mutableListOf<Marker>()
        if(jsonString != ""){
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val latitude = jsonObject.getDouble("latitude")
                val longitude = jsonObject.getDouble("longitude")
                val title = jsonObject.getString("title")

                val marker = Marker(map)
                marker.position = GeoPoint(latitude, longitude)
                marker.title = title
                markers.add(marker)
            }
        }
        else{
            return emptyList()
        }
        return markers
    }*/
    // Save markers to SharedPreferences
    // Save markers to SharedPreferences
    fun saveMarkersToSharedPreferences(context: Context, markers: List<Marker>) {
        val sharedPreferences = context.getSharedPreferences("markers", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        // Convert the list of markers to a set of serialized strings
        val markerSet = mutableSetOf<String>()
        markers.forEach { marker ->
            val jsonObject = JSONObject()
            jsonObject.put("latitude", marker.position.latitude)
            jsonObject.put("longitude", marker.position.longitude)
            jsonObject.put("title", marker.title)
            markerSet.add(jsonObject.toString())
        }

        // Save the set of serialized strings to SharedPreferences
        editor.putStringSet("markerSet", markerSet)
        editor.apply()
    }

    // Load markers from SharedPreferences
    fun loadMarkersFromSharedPreferences(context: Context): List<Marker> {
        val sharedPreferences = context.getSharedPreferences("markers", Context.MODE_PRIVATE)

        // Get the set of serialized strings from SharedPreferences
        val markerSet = sharedPreferences.getStringSet("markerSet", null)

        // Convert the set of serialized strings to a list of markers
        val markers = mutableListOf<Marker>()
        markerSet?.forEach { markerString ->
            val jsonObject = JSONObject(markerString)
            val latitude = jsonObject.getDouble("latitude")
            val longitude = jsonObject.getDouble("longitude")
            val title = jsonObject.getString("title")

            val marker = Marker(map)
            marker.position = GeoPoint(latitude, longitude)
            marker.title = title
            markers.add(marker)
        }

        return markers
    }




}

