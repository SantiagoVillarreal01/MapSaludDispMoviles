package ec.edu.mapsalud.dto

data class CitaDetalle(
    val appointment: CitaDtoRemote,
    val doctor: Medico,
    val office: ConsultorioDtoRemote
)