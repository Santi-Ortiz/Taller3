package com.example.taller3

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.Handler
import android.os.IBinder
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class LocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val locationUpdateHandler = Handler()
    private val LOCATION_UPDATE_INTERVAL: Long = 6000 // Actualiza cada 1 minuto
    private lateinit var auth: FirebaseAuth

    override fun onCreate() {
        super.onCreate()
        auth = FirebaseAuth.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        startLocationUpdates()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Indicar que el servicio debe reiniciarse si se mata
        return START_STICKY
    }

    private fun startLocationUpdates() {
        locationUpdateHandler.post(locationUpdateRunnable)
    }

    private val locationUpdateRunnable = object : Runnable {
        override fun run() {
            updateCurrentLocationInFirebase()
            locationUpdateHandler.postDelayed(this, LOCATION_UPDATE_INTERVAL)
        }
    }

    @SuppressLint("MissingPermission")
    private fun updateCurrentLocationInFirebase() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                val currentLatLng = mapOf(
                    "latitude" to it.latitude,
                    "longitude" to it.longitude
                )

                // Actualizar la ubicación del usuario en Firebase
                auth.currentUser?.let { user ->
                    val database = FirebaseDatabase.getInstance()
                    val userRef = database.getReference("usuarios").child(user.uid)
                    userRef.child("location").setValue(currentLatLng)
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        locationUpdateHandler.removeCallbacks(locationUpdateRunnable)
    }
}
