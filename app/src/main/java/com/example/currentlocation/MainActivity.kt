package com.example.currentlocation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import okhttp3.*
import java.io.IOException

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        mMap.uiSettings.isMyLocationButtonEnabled = true

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 1)
            return
        }

        mMap.isMyLocationEnabled = true

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                val currentLatLng = LatLng(it.latitude, it.longitude)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))

                fetchBirdHotspots(it.latitude, it.longitude)
            }
        }
    }

    private fun fetchBirdHotspots(lat: Double, lng: Double) {
        val apiKey = "biekti07bd7m"  // Replace this with your actual API key
        val client = OkHttpClient()

        val request = Request.Builder()
            .url("https://api.ebird.org/v2/ref/hotspot/geo?lat=$lat&lng=$lng&fmt=json")
            .header("X-eBirdApiToken", apiKey)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("EBIRD_API", "Failed to fetch data: ", e)
            }

            override fun onResponse(call: Call, response: Response) {
                val jsonData = response.body?.string()
                jsonData?.let {
                    val jsonArray: JsonArray = JsonParser.parseString(it).asJsonArray

                    for (jsonElement in jsonArray) {
                        val jsonObject = jsonElement.asJsonObject
                        val lat = jsonObject.get("lat").asDouble
                        val lng = jsonObject.get("lng").asDouble
                        val locName = jsonObject.get("locName").asString

                        runOnUiThread {
                            mMap.addMarker(MarkerOptions().position(LatLng(lat, lng)).title(locName))
                        }
                    }
                }
            }
        })
    }
}
