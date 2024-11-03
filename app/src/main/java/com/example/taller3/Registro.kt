package com.example.taller3

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.taller3.databinding.ActivityRegistroBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.auth
import com.google.firebase.database.database

class Registro : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth;
    private lateinit var binding: ActivityRegistroBinding

    companion object {
        const val PATH_USERS = "usuarios/"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_registro)

        binding = ActivityRegistroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth
        binding.btnRegister.setOnClickListener {
            registrarUsuario(binding.emailRegister.text.toString(), binding.passwordRegister.text.toString())
        }
    }

    private fun updateUI(currentUser: FirebaseUser?){

        if (currentUser != null) {
            val intent = Intent(this, Principal::class.java)
            intent.putExtra("user", currentUser.email)
            startActivity(intent)
        } else {
            binding.emailRegister.setText("")
            binding.passwordRegister.setText("")
        }
    }

    private fun escribirUsuario(){
        val user = Usuario()
        val database = Firebase.database

        user.nombre = binding.nombreRegister.text.toString()
        user.apellido = binding.apellidoRegister.text.toString()
        user.email = binding.emailRegister.text.toString()
        user.password = binding.passwordRegister.text.toString()
        user.identificacion = binding.idRegister.text.toString().toInt()
        user.latitud = 0.0
        user.longitud = 0.0
        user.imageUrl = ""
        val myRef = database.getReference(PATH_USERS+auth.currentUser!!.uid)
        myRef.setValue(user)
    }

    private fun registrarUsuario(correo: String, contrasena: String){
        auth.createUserWithEmailAndPassword(correo, contrasena)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "createUserWithEmail:onComplete:" + task.isSuccessful)
                    val user = auth.currentUser
                    if (user != null) {
                        val upcrb = UserProfileChangeRequest.Builder()
                        upcrb.setDisplayName(binding.nombreRegister.text.toString() + " " + binding.apellidoRegister.text.toString())
                        //upcrb.setPhotoUri(Uri.parse("path/to/pic"))
                        user.updateProfile(upcrb.build())
                        escribirUsuario()
                        updateUI(user)
                    }
                } else {
                    Toast.makeText(this, "createUserWithEmail:Failure: " + task.exception.toString(),
                        Toast.LENGTH_SHORT).show()
                    task.exception?.message?.let { Log.e(TAG, it) }
                }
            }
    }

}