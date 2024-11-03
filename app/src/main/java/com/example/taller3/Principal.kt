package com.example.taller3

import android.Manifest
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import org.json.JSONObject
import java.io.InputStream
import com.google.android.gms.maps.model.BitmapDescriptorFactory

class Principal : AppCompatActivity(), OnMapReadyCallback {

    // Variables para el mapa y la actualización de la ubicación
    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val LOCATION_PERMISSION_REQUEST = 1
    private val locationUpdateHandler = Handler(Looper.getMainLooper())
    private lateinit var auth: FirebaseAuth

    companion object {
        const val PATH_USERS = "usuarios/"
        const val LOCATION_UPDATE_INTERVAL: Long = 60000
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_principal)

        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)

        auth = FirebaseAuth.getInstance()
        // loadUsers()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        checkLocationPermission()
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST
            )
        } else {
            getCurrentLocation()
        }
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    val currentLatLng = LatLng(it.latitude, it.longitude)

                    // Guardar la ubicación del usuario en la base de datos de Firebase
                    auth.currentUser?.let { user ->
                        val database = FirebaseDatabase.getInstance()
                        val userRef = database.getReference(PATH_USERS).child(user.uid)
                        userRef.child("location").setValue(mapOf(
                            "latitude" to it.latitude,
                            "longitude" to it.longitude
                        ))
                    }

                    // Agregar marcador de la ubicación actual del usuario
                    mMap.addMarker(MarkerOptions().position(currentLatLng).title("Mi ubicación"))
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))

                    // Llamar a la función para cargar puntos de interés después de la ubicación actual
                    loadPointsOfInterest()
                }
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            mMap.isMyLocationEnabled = true
            startLocationUpdates()
        } else {
            checkLocationPermission()
        }
    }

    // Inicia las actualizaciones de ubicación en intervalos
    private fun startLocationUpdates() {
        locationUpdateHandler.post(locationUpdateRunnable)
    }

    private val locationUpdateRunnable = object : Runnable {
        override fun run() {
            updateCurrentLocationInFirebase()
            locationUpdateHandler.postDelayed(this, LOCATION_UPDATE_INTERVAL)
        }
    }

    private fun updateCurrentLocationInFirebase() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    val currentLatLng = LatLng(it.latitude, it.longitude)

                    // Actualizar la ubicación del usuario en Firebase
                    auth.currentUser?.let { user ->
                        val database = FirebaseDatabase.getInstance()
                        val userRef = database.getReference(PATH_USERS).child(user.uid)
                        userRef.child("location").setValue(mapOf(
                            "latitude" to it.latitude,
                            "longitude" to it.longitude
                        ))
                    }

                    // Añadir o actualizar el marcador de la ubicación actual en el mapa
                    mMap.clear()  // Limpiar para evitar duplicados
                    mMap.addMarker(
                        MarkerOptions()
                            .position(currentLatLng)
                            .title("Mi ubicación")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                    )
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))

                    // Cargar puntos de interés después de colocar la ubicación
                    loadPointsOfInterest()
                }
            }
        }
    }

    private fun loadPointsOfInterest() {
        try {
            val inputStream: InputStream = assets.open("locations.json")
            val jsonString = inputStream.bufferedReader().use { it.readText() }

            // Log para verificar el contenido del JSON
            Log.d(TAG, "Contenido del archivo JSON: $jsonString")

            val jsonObject = JSONObject(jsonString)
            val locationsArray = jsonObject.getJSONArray("locationsArray")

            for (i in 0 until locationsArray.length()) {
                val locationObject = locationsArray.getJSONObject(i)
                val latitude = locationObject.getDouble("latitude")
                val longitude = locationObject.getDouble("longitude")
                val name = locationObject.getString("name")

                val locationLatLng = LatLng(latitude, longitude)
                mMap.addMarker(
                    MarkerOptions()
                        .position(locationLatLng)
                        .title(name)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error al cargar los puntos de interés", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getCurrentLocation()
                } else {
                    Toast.makeText(
                        this,
                        "Se requiere permiso de ubicación para mostrar el mapa",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun loadUsers() {
        val database = Firebase.database
        val myRef = database.getReference(PATH_USERS)
        myRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (singleSnapshot in dataSnapshot.children) {
                    val usuario = singleSnapshot.getValue(Usuario::class.java)
                    Log.i(TAG, "Encontró usuario: " + usuario?.nombre)
                    val nombre = usuario?.nombre
                    val correo = usuario?.email
                    Toast.makeText(baseContext, "$nombre: $correo", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {
                Log.w(TAG, "Error en la consulta", databaseError.toException())
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        return when (item.itemId) {
            R.id.menuCerrarSesion -> {
                auth.signOut()
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)
                true
            }
            R.id.menuDisponible -> {
                user?.let {
                    val database = Firebase.database
                    val myRef = database.getReference(PATH_USERS).child(it.uid)
                    myRef.child("disponible").setValue(true)
                }
                true
            }
            R.id.menuDesconectado -> {
                user?.let {
                    val database = Firebase.database
                    val myRef = database.getReference(PATH_USERS).child(it.uid)
                    myRef.child("disponible").setValue(false)
                }
                true
            }
            R.id.menuOtrosUsuarios -> {
                val intent = Intent(this, VerUsuarios::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

}