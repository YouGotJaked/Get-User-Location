package com.example.jakeday.kotlindemo

import android.content.pm.PackageManager
import android.location.Location
import android.location.Address
import android.location.Geocoder
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.TextInputEditText
import android.support.v4.app.ActivityCompat
import android.util.Log
import android.widget.Button
import android.widget.Toast
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.gson.GsonBuilder
import java.io.IOException
import kotlinx.android.synthetic.main.activity_maps.*
import kotlin.reflect.full.memberProperties

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {
    override fun onMarkerClick(p0: Marker?) = false

    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var lastLocation: Location

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        map.uiSettings.isZoomControlsEnabled = true
        map.setOnMarkerClickListener(this)

        setUpMap()
    }

    private fun setUpMap() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
            return
        }

        map.isMyLocationEnabled = true

        fusedLocationClient.lastLocation.addOnSuccessListener(this) { location ->
            if (location != null) {
                lastLocation = location
                val currentLatLng = LatLng(location.latitude, location.longitude)
                placeMarkerOnMap(currentLatLng)
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 12f))
                buttonSubmit(R.id.submit, currentLatLng)
            }
        }
    }

    private fun placeMarkerOnMap(location: LatLng) {
        val markerOptions = MarkerOptions().position(location)

        val titleStr = getAddressString(getAddress(location))
        markerOptions.title(titleStr)

        map.addMarker(markerOptions)
    }

    private fun getAddress(latLng: LatLng): List<Address>? {
        val geocoder = Geocoder(this)
        var addresses: List<Address>? = emptyList()

        try {
            addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
        } catch (e: IOException) {
            Log.e("MapsActivity", e.localizedMessage)
        }

        return addresses
    }

    private fun getAddressString(addresses: List<Address>?): String {
        var addressText = ""
        val address: Address?

        if (null != addresses && !addresses.isEmpty()) {
            address = addresses[0]
            for (i in 0 until address.maxAddressLineIndex) {
                addressText += if (i == 0) address.getAddressLine(i) else "\n" + address.getAddressLine(i)
            }
            prefillForm(addresses)
        }

        return addressText
    }

    private fun prefillForm(addresses: List<Address>) {
        street.setText(addresses[0].subThoroughfare + " " + addresses[0].thoroughfare)
        city.setText(addresses[0].locality)
        state.setText(addresses[0].adminArea)
        zip.setText(addresses[0].postalCode)
    }

    private fun parseForm(input: Form): String? {
        for (prop in Form::class.memberProperties) {
            if (prop.get(input).toString().isNullOrBlank()) {
                return null
            }
        }

        val gson = GsonBuilder().setPrettyPrinting().create()
        return gson.toJson(input)
    }

    private fun buttonSubmit(buttonId: Int, currentLatLng: LatLng) {
        val buttonClick = findViewById<Button>(buttonId)
        buttonClick.setOnClickListener {
            val form = fillForm(getAddress(currentLatLng))
            val parsed = parseForm(form)
            var display = ""
            display = if (parsed.isNullOrBlank()) {
                "Form not submitted. One or more fields are null or blank."
            } else {
                "Form submitted:\n $parsed"
            }
            Toast.makeText(this@MapsActivity, display, Toast.LENGTH_SHORT).show()
        }
    }

    private fun fillForm(address: List<Address>?): Form {
        val first = findViewById<TextInputEditText>(R.id.first)
        val last = findViewById<TextInputEditText>(R.id.last)
        val street = address!![0].subThoroughfare + " " + address[0].thoroughfare
        val city = address[0].locality
        val state = address[0].adminArea
        val zip = address[0].postalCode

        return Form(first.text.toString(), last.text.toString(), street, city, state, zip)
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }
}

class Form constructor(var first: String, var last: String, var street: String, var city: String, var state: String, var zip: String) {
    override fun toString(): String = "$first $last\n$street\n$city, $state $zip"
}