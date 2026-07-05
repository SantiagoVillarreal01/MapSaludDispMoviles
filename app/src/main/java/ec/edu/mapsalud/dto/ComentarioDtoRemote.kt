package ec.edu.mapsalud.dto

data class ComentarioDtoRemote(
    val id: String = "",
    val idCenter: String = "",
    val idUser: String = "",
    val rating: Double = 0.0,
    val review: String = ""
)