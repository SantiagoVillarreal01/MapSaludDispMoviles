package ec.edu.mapsalud.dto

data class AppointmentDtoRemote(

    val id: String = "",
    val idUser: String = "",
    val idDoctor: String = "",
    val idOffice: String = "",
    val idCenter: String = "",
    val reason: String = "",
    val description: String = "",
    val date: String = "", //dd/MM/yyyy
    val time: String = "",
    val status: String = "Pendiente", // "Pendiente", "Completada", "Cancelada", "Inasistencia"
    val diagnosis: DiagnosisEmbedded? = null // Empieza en null hasta que el médico lo llena

)