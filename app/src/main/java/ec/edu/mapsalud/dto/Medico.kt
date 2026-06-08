package ec.edu.mapsalud.dto

data class Medico(
    val nombres: String,
    val apellidos: String,
    val correo: String,
    val correoMapsalud: String,
    val contrasenaMapsalud: String,
    val telefono: String,
    val cedula: String,
    val idEspecialidadPrincipal: Int,
    val anosExperiencia: Int
)