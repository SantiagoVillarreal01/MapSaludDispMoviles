package ec.edu.mapsalud.dto

data class CitaPaciente(
    val appointment: CitaDtoRemote,
    val paciente: Paciente,
    val horaFormateada: String,
    val amPm: String
)