package com.example.taller3

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database

class Principal : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    companion object {
        const val PATH_USERS = "usuarios/"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_principal)

        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)

        auth = FirebaseAuth.getInstance()
        //loadUsers()
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
                Toast.makeText(this, "Usuario disponible", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.menuDesconectado -> {
                user?.let {
                    val database = Firebase.database
                    val myRef = database.getReference(PATH_USERS).child(it.uid)
                    myRef.child("disponible").setValue(false)
                }
                Toast.makeText(this, "Usuario desconectado", Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}