package com.example.taller3

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class MapaActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var auth: FirebaseAuth
    private lateinit var userEmail: String // Email del usuario que se está siguiendo
    private lateinit var database: DatabaseReference

    private val locationUpdateListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val lat = snapshot.child("location/latitude").getValue(Double::class.java)
            val lng = snapshot.child("location/longitude").getValue(Double::class.java)

            if (lat != null && lng != null) {
                Log.d("MapaActivity", "Ubicación obtenida: ($lat, $lng)")
                // Aquí se añade el marcador y se centra el mapa
            } else {
                Log.d("MapaActivity", "Ubicación no encontrada en la base de datos")
            }
        }

        override fun onCancelled(error: DatabaseError) {
            Log.e("MapaActivity", "Error al cargar ubicación: ${error.message}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mapa)

        // Obtener el ID del usuario que se está siguiendo desde el intent
        userEmail = intent.getStringExtra("USER_EMAIL") ?: ""
        auth = FirebaseAuth.getInstance()

        // Formatear el correo para usarlo como clave en Firebase
        val formattedEmail = formatEmailForFirebase(userEmail)
        database = FirebaseDatabase.getInstance().reference.child("usuarios").child(formattedEmail)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Configurar el mapa
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun formatEmailForFirebase(email: String): String {
        return email.replace(".", ",") // Cambia el punto por una coma
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Verificar permisos de ubicación
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        } else {
            mMap.isMyLocationEnabled = true
        }

        // Escuchar cambios en la ubicación del usuario
        database.child("location").addValueEventListener(locationUpdateListener)
    }

    @SuppressLint("MissingPermission")
    private fun calcularDistancia(lat: Double, lng: Double) {
        // Obtener la ubicación del usuario que está haciendo el seguimiento
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                val currentLocation = LatLng(it.latitude, it.longitude)
                val userLocation = LatLng(lat, lng)
                val distance = FloatArray(1)
                Location.distanceBetween(currentLocation.latitude, currentLocation.longitude, lat, lng, distance)
                // Mostrar la distancia en línea recta
                Log.d("MapaActivity", "Distancia: ${distance[0]} metros")
                // Aquí puedes actualizar un TextView o algo similar para mostrar la distancia
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        database.child("location").removeEventListener(locationUpdateListener)
    }
}
