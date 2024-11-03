package com.example.taller3

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.taller3.databinding.ActivityRegistroBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.auth
import com.google.firebase.database.database
import com.google.firebase.storage.storage

class Registro : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityRegistroBinding
    private val storageRef = Firebase.storage.reference
    private var imageUri: Uri? = null

    companion object {
        const val PATH_USERS = "usuarios/"
        const val REQUEST_IMAGE_PICK = 1001
        const val PERMISSION_REQUEST_CODE = 123
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth

        binding.btnRegister.isEnabled = false
        binding.btnUploadImage.isEnabled = false

        checkPermissions()

        binding.btnRegister.setOnClickListener {
            registrarUsuario(binding.emailRegister.text.toString(), binding.passwordRegister.text.toString())
        }
        binding.btnUploadImage.setOnClickListener {
            seleccionarImagen()
        }
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_MEDIA_IMAGES
                ) == PackageManager.PERMISSION_GRANTED -> {
                    enableButtons()
                }
                shouldShowRequestPermissionRationale(Manifest.permission.READ_MEDIA_IMAGES) -> {
                    showPermissionDeniedMessage()
                }
                else -> {
                    requestPermissions(
                        arrayOf(Manifest.permission.READ_MEDIA_IMAGES),
                        PERMISSION_REQUEST_CODE
                    )
                }
            }
        } else {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED -> {
                    enableButtons()
                }
                shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE) -> {
                    showPermissionDeniedMessage()
                }
                else -> {
                    requestPermissions(
                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                        PERMISSION_REQUEST_CODE
                    )
                }
            }
        }
    }

    private fun enableButtons() {
        binding.btnRegister.isEnabled = true
        binding.btnUploadImage.isEnabled = true
    }

    private fun showPermissionDeniedMessage() {
        Toast.makeText(this, "¡No podrás registrarte!", Toast.LENGTH_LONG).show()
        binding.btnRegister.isEnabled = false
        binding.btnUploadImage.isEnabled = false
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    enableButtons()
                } else {
                    showPermissionDeniedMessage()
                }
            }
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
        user.imageUrl = imageUri?.toString() ?: ""
        user.latitud = 0.0
        user.longitud = 0.0
        user.disponible = true

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
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK && data != null) {
            imageUri = data.data
            if (imageUri != null) {
                binding.imageContact.setImageURI(imageUri)
                subirImagen(imageUri!!)
            }
        }
    }

    private fun subirImagen(uri: Uri) {
        val userId = auth.currentUser
        if (userId == null) {
            Toast.makeText(this, "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        val imageRef = storageRef.child("usuarios/${userId.uid}/profile_image.jpg")

        imageRef.putFile(uri)
            .addOnSuccessListener { taskSnapshot ->
                imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    Log.i(TAG, "URL de la imagen: $downloadUri")
                    Toast.makeText(this, "Imagen subida correctamente", Toast.LENGTH_SHORT).show()
                    guardarUrlImagen(downloadUri.toString())
                    imageUri = downloadUri
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error al subir la imagen: ${exception.message}")
                Toast.makeText(this, "Error al subir la imagen", Toast.LENGTH_SHORT).show()
            }
    }

    private fun guardarUrlImagen(url: String) {
        val userId = auth.currentUser
        if (userId != null) {
            val myRef = Firebase.database.getReference(PATH_USERS).child(userId.uid)

            myRef.child("imageUrl").setValue(url)
                .addOnSuccessListener {
                    Log.i(TAG, "URL de la imagen guardada correctamente en Realtime Database")
                    Toast.makeText(this, "URL de la imagen guardada en la base de datos", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Error al guardar la URL de la imagen en Realtime Database: ${exception.message}")
                    Toast.makeText(this, "Error al guardar la URL de la imagen", Toast.LENGTH_SHORT).show()
                }
        } else {
            Log.e(TAG, "El usuario no está autenticado. No se puede guardar la URL de la imagen.")
        }
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
                        upcrb.setPhotoUri(Uri.parse("usuarios/" + user.uid + "/imageUrl/image.jpg"))
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