package com.example.taller3

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.auth.FirebaseAuth

class VerUsers : AppCompatActivity() {
    private lateinit var listViewUsuarios: ListView
    private lateinit var usuariosAdapter: UsersAdapter
    private val listaUsuarios = mutableListOf<Usuario>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ver_users)

        listViewUsuarios = findViewById(R.id.listViewUsuarios)

        // Inicializar el adaptador personalizado
        usuariosAdapter = UsersAdapter(this, listaUsuarios) { usuario ->
            // Acción al hacer clic en el botón "Ver posición" para cada usuario
        }
        listViewUsuarios.adapter = usuariosAdapter

        // Iniciar el servicio de estado de usuario
        val intent = Intent(this, UserStatusService::class.java)
        startService(intent)

        // Cargar la lista de usuarios desde Firebase
        cargarUsuariosDeFirebase()
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
}
