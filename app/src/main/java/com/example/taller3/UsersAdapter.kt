package com.example.taller3

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.storage.FirebaseStorage

class UsersAdapter(
    private val context: Context,
    private val usuarios: List<Usuario>,
    private val onVerPosicionClicked: (Usuario) -> Unit
) : ArrayAdapter<Usuario>(context, R.layout.layout_users, usuarios) {

    private val storage = FirebaseStorage.getInstance()
    private val requestOptions = RequestOptions()
        .diskCacheStrategy(DiskCacheStrategy.ALL)
        .placeholder(R.drawable.fotoperfil)
        .error(R.drawable.fotoperfil)

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.layout_users, parent, false)

        val usuario = usuarios[position]

        val imgContact = view.findViewById<ImageView>(R.id.imgContact)
        val nombreTextView = view.findViewById<TextView>(R.id.nombreTextView)
        val verPosicionButton = view.findViewById<Button>(R.id.btnVerPosicion)

        // Configurar el nombre del usuario
        nombreTextView.text = "${usuario.nombre} ${usuario.apellido}"

        // Manejar la carga de la imagen
        cargarImagenPerfil(usuario, imgContact)

        verPosicionButton.setOnClickListener {
            onVerPosicionClicked(usuario)
        }

        return view
    }

    private fun cargarImagenPerfil(usuario: Usuario, imageView: ImageView) {
        val imageUrl = usuario.imageUrl

        when {
            // Si la URL es una referencia a Storage (comienza con "usuarios/")
            imageUrl?.startsWith("usuarios/") == true -> {
                storage.reference.child(imageUrl).downloadUrl
                    .addOnSuccessListener { uri ->
                        cargarImagenConGlide(uri.toString(), imageView)
                        Log.d("UsersAdapter", "URL de Storage obtenida: $uri")
                    }
                    .addOnFailureListener { exception ->
                        Log.e("UsersAdapter", "Error al obtener URL de Storage: ${exception.message}")
                        imageView.setImageResource(R.drawable.fotoperfil)
                    }
            }
            // Si ya es una URL completa (comienza con http:// o https://)
            imageUrl?.startsWith("http") == true -> {
                cargarImagenConGlide(imageUrl, imageView)
                Log.d("UsersAdapter", "Cargando URL directa: $imageUrl")
            }
            // Si no hay URL o es inválida
            else -> {
                Log.d("UsersAdapter", "No hay URL de imagen válida, usando imagen por defecto")
                imageView.setImageResource(R.drawable.fotoperfil)
            }
        }
    }

    private fun cargarImagenConGlide(url: String, imageView: ImageView) {
        Glide.with(context)
            .load(url)
            .apply(requestOptions)
            .into(imageView)
    }
}