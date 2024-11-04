package com.example.taller3

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.database

class VerUsers : AppCompatActivity() {
    private lateinit var listViewUsuarios: ListView
    private lateinit var usuariosAdapter: UsersAdapter
    private val listaUsuarios = mutableListOf<Usuario>()
    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ver_users)
        auth = FirebaseAuth.getInstance()

        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)

        listViewUsuarios = findViewById(R.id.listViewUsuarios)

        // Inicializar el adaptador personalizado
        usuariosAdapter = UsersAdapter(this, listaUsuarios) { usuario ->
            val intent = Intent(this, MapaActivity::class.java)
            intent.putExtra("USER_EMAIL", usuario.email) // Pasar el email del usuario
            startActivity(intent)
        }

        listViewUsuarios.adapter = usuariosAdapter

        // Iniciar el servicio de estado de usuario
        val intent = Intent(this, UserStatusService::class.java)
        startService(intent)

        // Cargar la lista de usuarios desde Firebase
        cargarUsuariosDeFirebase()

        // Verifica y solicita permisos de ubicación
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        } else {
            startLocationService()
        }
    }

    private fun startLocationService() {
        val intent = Intent(this, LocationService::class.java)
        startService(intent)
    }

    private fun cargarUsuariosDeFirebase() {
        val database = FirebaseDatabase.getInstance().reference.child("usuarios")

        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                listaUsuarios.clear()
                val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email

                for (usuarioSnapshot in snapshot.children) {
                    val usuario = usuarioSnapshot.getValue(Usuario::class.java)
                    if (usuario != null && usuario.disponible == true && usuario.email != currentUserEmail) {
                        listaUsuarios.add(usuario)
                        Log.d("VerUsers", "Usuario agregado: $usuario")
                    }
                }
                usuariosAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("VerUsers", "Error al cargar usuarios: ${error.message}")
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu2, menu)
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
            R.id.menuMiMapa -> {
                val intent = Intent(this, Principal::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
