package ec.edu.mapsalud.dto

data class Paciente(
    val nombres: String,
    val apellidos: String,
    val correo: String,
    val contrasena: String,
    val telefono: String,
    val cedula: String
)