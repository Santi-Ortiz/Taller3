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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage

class Registro : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityRegistroBinding
    private val storageRef = FirebaseStorage.getInstance().reference
    private var imageUri: Uri? = null
    private val database = FirebaseDatabase.getInstance().reference

    companion object {
        const val PATH_USERS = "usuarios/"
        const val REQUEST_IMAGE_PICK = 1001
        const val PERMISSION_REQUEST_CODE = 123
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

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
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED) {
                enableButtons()
            } else {
                requestPermissions(arrayOf(Manifest.permission.READ_MEDIA_IMAGES), PERMISSION_REQUEST_CODE)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                enableButtons()
            } else {
                requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), PERMISSION_REQUEST_CODE)
            }
        }
    }

    private fun enableButtons() {
        binding.btnRegister.isEnabled = true
        binding.btnUploadImage.isEnabled = true
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
            binding.imageContact.setImageURI(imageUri)
        }
    }

    private fun registrarUsuario(correo: String, contrasena: String) {
        auth.createUserWithEmailAndPassword(correo, contrasena)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Usuario creado exitosamente")
                    val user = auth.currentUser
                    if (user != null) {
                        val upcrb = UserProfileChangeRequest.Builder()
                            .setDisplayName(binding.nombreRegister.text.toString() + " " + binding.apellidoRegister.text.toString())
                            .build()
                        user.updateProfile(upcrb)
                        subirImagen(imageUri!!)
                        escribirUsuario(null)
                    }
                } else {
                    Toast.makeText(this, "Error en el registro: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    Log.e(TAG, task.exception?.message ?: "Error desconocido")
                }
            }
    }

    private fun subirImagen(uri: Uri) {
        val user = auth.currentUser
        val imageRef = storageRef.child("images/${user!!.uid}.jpg")

        imageRef.putFile(uri)
            .addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    val imageUrl = downloadUri.toString()
                    Log.i(TAG, "URL de la imagen: $imageUrl")
                    Toast.makeText(this, "Imagen subida correctamente", Toast.LENGTH_SHORT).show()
                    user.updateProfile(UserProfileChangeRequest.Builder().setPhotoUri(downloadUri).build())
                    user.let {
                        val userRef = database.child("usuarios").child(it.uid)
                        userRef.child("imageUrl").setValue(imageUrl)
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error al subir la imagen: ${exception.message}")
                Toast.makeText(this, "Error al subir la imagen", Toast.LENGTH_SHORT).show()
            }
    }

    private fun escribirUsuario(imageUrl: String?) {
        val user = Usuario()

        user.nombre = binding.nombreRegister.text.toString()
        user.apellido = binding.apellidoRegister.text.toString()
        user.email = binding.emailRegister.text.toString()
        user.password = binding.passwordRegister.text.toString()
        user.identificacion = binding.idRegister.text.toString().toInt()
        user.imageUrl = imageUri?.toString() ?: ""
        user.disponible = true

        val database = FirebaseDatabase.getInstance().getReference(PATH_USERS + auth.currentUser!!.uid)
        database.setValue(user).addOnCompleteListener {
            if (it.isSuccessful) {
                Log.i(TAG, "Usuario guardado correctamente en Realtime Database")
                Toast.makeText(this, "Registro exitoso", Toast.LENGTH_SHORT).show()
                updateUI(auth.currentUser)
            } else {
                Log.e(TAG, "Error al guardar el usuario en Realtime Database")
                Toast.makeText(this, "Error al guardar en base de datos", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateUI(currentUser: FirebaseUser?) {
        if (currentUser != null) {
            val intent = Intent(this, Principal::class.java)
            intent.putExtra("user", currentUser.email)
            startActivity(intent)
        } else {
            binding.emailRegister.setText("")
            binding.passwordRegister.setText("")
        }
    }
}
