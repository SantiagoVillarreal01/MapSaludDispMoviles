package ec.edu.mapsalud.dto

data class AppointmentDetail(
    val appointment: AppointmentDtoRemote,
    val doctor: Medico,
    val office: OfficeDtoRemote
)