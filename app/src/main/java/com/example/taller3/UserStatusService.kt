package com.example.taller3

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class UserStatusService : Service() {
    private lateinit var database: DatabaseReference
    private lateinit var userEmail: String

    override fun onCreate() {
        super.onCreate()
        // Inicializar la referencia de la base de datos
        database = FirebaseDatabase.getInstance().reference.child("usuarios")
        // Obtener el ID del usuario actual
        userEmail = FirebaseAuth.getInstance().currentUser?.email ?: ""
        // Escuchar cambios en la base de datos
        escucharCambiosEnUsuarios()
    }

    private fun escucharCambiosEnUsuarios() {
        database.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val usuario = snapshot.getValue(Usuario::class.java)
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val usuario = snapshot.getValue(Usuario::class.java)
                if (usuario != null) {
                    // Verificar si el usuario se ha conectado o desconectado
                    if (usuario.disponible && usuario.email != userEmail) {
                        Toast.makeText(applicationContext, "${usuario.nombre} se ha conectado.", Toast.LENGTH_SHORT).show()
                    } else if (!usuario.disponible) {
                        Toast.makeText(applicationContext, "${usuario.nombre} se ha desconectado.", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                val usuario = snapshot.getValue(Usuario::class.java)
                if (usuario != null) {
                    Toast.makeText(applicationContext, "${usuario.nombre} se ha desconectado.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                // Este método no se utiliza en este caso
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("UserStatusService", "Error al escuchar cambios: ${error.message}")
            }
        })
    }

    private fun stopLocationService() {
        val serviceIntent = Intent(this, LocationService::class.java)
        stopService(serviceIntent)
    }


    override fun onBind(intent: Intent?): IBinder? {
        // No se utiliza en este caso
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
