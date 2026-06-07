package com.example.mapsalud20.dto

data class CitaParaDiagnostico(
    val idCita: String,
    val fecha: String,
    val paciente: String,
    val motivo: String,
    val colorBorde: String = "#00838F"
)