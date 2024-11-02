package com.example.taller3

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth

class IniciarSesion : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth;

    private lateinit var correoInput: EditText
    private lateinit var contrasenaInput: EditText
    private lateinit var iniciarSesionButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_iniciar_sesion)

        auth = Firebase.auth

        iniciarSesionButton = findViewById(R.id.btnLogin)
        correoInput = findViewById(R.id.emailLogin)
        contrasenaInput = findViewById(R.id.passwordLogin)

        iniciarSesionButton.setOnClickListener {
            val email = correoInput.text.toString()
            val password = contrasenaInput.text.toString()
            if (validateForm() && isEmailValid(email)) {
                signInUser(email, password)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        updateUI(currentUser)
    }

    private fun updateUI(currentUser: FirebaseUser?){

        if (currentUser != null) {
            val intent = Intent(this, Principal::class.java)
            intent.putExtra("user", currentUser.email)
            startActivity(intent)
        } else {
            correoInput.setText("")
            contrasenaInput.setText("")
        }
    }

    private fun validateForm(): Boolean {
        var valid = true
        val email = correoInput.text.toString()
        if (TextUtils.isEmpty(email)) {
            correoInput.error = "¡Campo requerido!"
            valid = false
        } else {
            correoInput.error = null
        }
        val password = contrasenaInput.text.toString()
        if (TextUtils.isEmpty(password)) {
            contrasenaInput.error = "¡Campo requerido!"
            valid = false
        } else {
            contrasenaInput.error = null
        }
        return valid
    }

    private fun isEmailValid(email: String): Boolean {
        val emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
        return email.matches(emailRegex.toRegex())
    }

    private fun signInUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithEmail:success")
                    val user = auth.currentUser
                    updateUI(user)
                } else {
                    Log.w(TAG, "signInWithEmail:failure", task.exception)
                    Toast.makeText(this, "Autenticación fallida. Inténtalo de nuevo", Toast.LENGTH_SHORT).show()
                    updateUI(null)
                }
            }
    }
}