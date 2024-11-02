package com.example.taller3;

public class Usuario {
    public String nombre;
    public String apellido;
    public String email;
    public int identificacion;
    public double latitud;
    public double longitud;
    public String imageUrl;

    public Usuario() {}

    public Usuario(String nombre, String apellido, String email, int identificacion,
                double latitud, double longitud, String imageUrl) {
        this.nombre = nombre;
        this.apellido = apellido;
        this.email = email;
        this.identificacion = identificacion;
        this.latitud = latitud;
        this.longitud = longitud;
        this.imageUrl = imageUrl;
    }
}
