package com.example.mapsalud20.dto


data class Diagnostico(
    val titulo: String, val doctorFecha: String, val recomendaciones: String, val medicamentos: List<Medicamento>)