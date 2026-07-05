package ec.edu.mapsalud.dto

data class ConsultorioDtoRemote(
    val id: String = "",
    val idCenter: String = "",
    val idDoctor: String = "",
    val specialty: String = "",
    val availableDays: List<String> = emptyList(),
    val openingTime: String = "",
    val closingTime: String = ""
)