// MapaActivity.kt
package com.example.taller3

import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class MapaActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private var userEmail: String? = null
    private lateinit var distanceTextView: TextView
    private var currentUserLocation: LatLng? = null
    private var targetUserLocation: LatLng? = null
    private var currentUserMarker: Marker? = null
    private var targetMarker: Marker? = null
    private var polyline: Polyline? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mapa)

        auth = FirebaseAuth.getInstance()
        userEmail = intent.getStringExtra("USER_EMAIL")

        // Configura el fragmento del mapa
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Inicializa la referencia a la base de datos
        database = FirebaseDatabase.getInstance().getReference("usuarios")

        // Referencia al TextView donde se mostrará la distancia
        distanceTextView = findViewById(R.id.distanceTextView)

        // Escuchar cambios de ubicación en tiempo real
        escucharCambiosEnUbicacionUsuarioActual()
        if (userEmail != null) {
            escucharCambiosEnUbicacionUsuario(userEmail!!)
        } else {
            Toast.makeText(this, "No se encontró el email del usuario a seguir", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
    }

    private fun escucharCambiosEnUbicacionUsuarioActual() {
        val currentUserEmail = auth.currentUser?.email

        if (currentUserEmail != null) {
            database.orderByChild("email").equalTo(currentUserEmail)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val usuarioSnapshot = snapshot.children.first()
                            val latitud = usuarioSnapshot.child("location").child("latitude").getValue(Double::class.java)
                            val longitud = usuarioSnapshot.child("location").child("longitude").getValue(Double::class.java)

                            if (latitud != null && longitud != null) {
                                val newLocation = LatLng(latitud, longitud)
                                currentUserLocation = newLocation
                                actualizarUbicacionActualEnMapa(newLocation)
                                recalcularDistanciaYLinea()
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("MapaActivity", "Error al obtener ubicación del usuario actual: ${error.message}")
                    }
                })
        }
    }

    private fun actualizarUbicacionActualEnMapa(location: LatLng) {
        if (currentUserMarker == null) {
            // Crea el marcador si no existe
            currentUserMarker = mMap.addMarker(
                MarkerOptions()
                    .position(location)
                    .title("Mi ubicación")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
            )
        } else {
            // Mueve el marcador a la nueva ubicación
            currentUserMarker?.position = location
        }
    }

    private fun escucharCambiosEnUbicacionUsuario(email: String) {
        database.orderByChild("email").equalTo(email)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val usuarioSnapshot = snapshot.children.first()
                        val latitud = usuarioSnapshot.child("location").child("latitude").getValue(Double::class.java)
                        val longitud = usuarioSnapshot.child("location").child("longitude").getValue(Double::class.java)

                        if (latitud != null && longitud != null) {
                            val targetLocation = LatLng(latitud, longitud)
                            targetUserLocation = targetLocation
                            actualizarUbicacionUsuarioEnMapa(targetLocation)
                            recalcularDistanciaYLinea()
                        }
                    } else {
                        Toast.makeText(this@MapaActivity, "Usuario no encontrado", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("MapaActivity", "Error al obtener ubicación: ${error.message}")
                }
            })
    }

    private fun actualizarUbicacionUsuarioEnMapa(location: LatLng) {
        if (targetMarker == null) {
            // Crea el marcador si no existe
            targetMarker = mMap.addMarker(
                MarkerOptions()
                    .position(location)
                    .title("Ubicación de ${userEmail}")
            )
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
        } else {
            // Mueve el marcador a la nueva ubicación
            targetMarker?.position = location
        }
    }

    private fun recalcularDistanciaYLinea() {
        if (currentUserLocation != null && targetUserLocation != null) {
            val currentUserLoc = Location("currentUser").apply {
                latitude = currentUserLocation!!.latitude
                longitude = currentUserLocation!!.longitude
            }

            val targetLoc = Location("targetUser").apply {
                latitude = targetUserLocation!!.latitude
                longitude = targetUserLocation!!.longitude
            }

            // Calcula la distancia
            val distancia = currentUserLoc.distanceTo(targetLoc)
            distanceTextView.text = "Distancia: ${"%.2f".format(distancia / 1000)} km"

            // Actualiza la línea entre el usuario actual y el usuario seguido
            polyline?.remove()
            polyline = mMap.addPolyline(
                PolylineOptions()
                    .add(currentUserLocation)
                    .add(targetUserLocation)
                    .width(5f)
                    .color(android.graphics.Color.BLUE)
            )
        }
    }
}
