package com.example.mapsalud.dto


data class Diagnostico(
    val titulo: String, val doctorFecha: String, val recomendaciones: String, val medicamentos: List<Medicamento>)