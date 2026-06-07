package com.example.mapsalud20.dto

data class CitaMedic(
    val hora: String,
    val amPm: String,
    val nombrePaciente: String,
    val motivo: String,
    val estado: String,      // Ej: "En espera", "Pendiente", "Urgente", "Confirmado"
    val colorBorde: String,  // Color hexadecimal para la línea izquierda
    val colorFondoEstado: String, // Color hexadecimal para la burbuja de estado
    val colorTextoEstado: String
)