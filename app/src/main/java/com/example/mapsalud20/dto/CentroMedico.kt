package com.example.mapsalud20.dto

data class CentroMedico(
    val nombre: String,
    val especialidad: String,
    val tipo: String, //"PUBLIC" o "PRIVATE"
    val distancia: String
)