package ec.edu.mapsalud.dto

data class AppointmentPaciente(
    val appointment: AppointmentDtoRemote,
    val paciente: Paciente,
    val horaFormateada: String,
    val amPm: String
)