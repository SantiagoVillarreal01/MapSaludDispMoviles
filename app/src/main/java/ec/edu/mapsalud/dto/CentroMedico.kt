package ec.edu.mapsalud.dto

data class CentroMedico(
    val nombre: String,
    val especialidad: String,
    val tipo: String, //"PUBLIC" o "PRIVATE"
    val distancia: String
)