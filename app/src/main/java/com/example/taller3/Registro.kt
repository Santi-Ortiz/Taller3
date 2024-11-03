package com.example.taller3

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.taller3.databinding.ActivityRegistroBinding
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.auth
import com.google.firebase.database.database
import com.google.firebase.storage.UploadTask
import com.google.firebase.storage.storage
import java.io.File

class Registro : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth;
    private lateinit var binding: ActivityRegistroBinding
    private val storageRef = Firebase.storage.reference

    companion object {
        const val PATH_USERS = "usuarios/"
        const val REQUEST_IMAGE_PICK = 1001
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
        binding.btnUploadImage.setOnClickListener {
            seleccionarImagen()
        }
    }

    @SuppressLint("SetTextI18n")
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

        val myRef = database.getReference(PATH_USERS+auth.currentUser!!.uid)
        myRef.setValue(user)
    }

    @SuppressLint("IntentReset")
    private fun seleccionarImagen() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_IMAGE_PICK)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_PICK && data != null) {
            val imageUri = data.data
            if (imageUri != null) {
                binding.imageContact.setImageURI(imageUri)
                subirImagen(imageUri)
            }
        }
    }

    private fun subirImagen(uri: Uri) {
        val userId = auth.currentUser
        val imageRef = storageRef.child("usuarios/" +userId!!.uid +"/imageUrl"+"image.jpg")

        imageRef.putFile(uri)
            .addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    Log.i(TAG, "URL de la imagen: $downloadUri")
                    Toast.makeText(this, "Imagen subida correctamente", Toast.LENGTH_SHORT).show()
                    guardarUrlImagen(downloadUri.toString())
                }
            }
            .addOnFailureListener {
                Log.e(TAG, "Error al subir la imagen: ${it.message}")
                Toast.makeText(this, "Error al subir la imagen", Toast.LENGTH_SHORT).show()
            }
    }

    private fun guardarUrlImagen(url: String) {
        val myRef = Firebase.database.getReference(PATH_USERS + auth.currentUser!!.uid)
        myRef.child("imageUrl").setValue(url)
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