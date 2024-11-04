package com.example.taller3

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import java.io.IOException

class UsersAdapter(
    private val context: Context,
    private val usuarios: List<Usuario>,
    private val onVerPosicionClicked: (Usuario) -> Unit
) : ArrayAdapter<Usuario>(context, R.layout.layout_users, usuarios) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.layout_users, parent, false)

        val usuario = usuarios[position]

        val imgContact = view.findViewById<ImageView>(R.id.imgContact)
        val nombreTextView = view.findViewById<TextView>(R.id.nombreTextView)
        val verPosicionButton = view.findViewById<Button>(R.id.btnVerPosicion)

        Log.d("UsersAdapter", "Cargando imagen desde URL: ${usuario.imageUrl}")

        // Concatenar nombre y apellido
        nombreTextView.text = "${usuario.nombre} ${usuario.apellido}" // Concatenar nombre y apellido

        verPosicionButton.setOnClickListener {
            onVerPosicionClicked(usuario)
        }

        return view
    }
}
