package com.example.taller3

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class Principal : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_principal)

        // Quitar el título
        supportActionBar?.setDisplayShowTitleEnabled(false)
        // Quitar la flecha de retroceso
        supportActionBar?.setDisplayHomeAsUpEnabled(false)

        auth = FirebaseAuth.getInstance()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menuCerrarSesion -> {
                auth.signOut()
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)
                true
            }
            R.id.menuDisponible -> {
                // Agrega alguna acción aquí
                true
            }
            R.id.menuDesconectado -> {
                // Agrega alguna acción aquí
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}