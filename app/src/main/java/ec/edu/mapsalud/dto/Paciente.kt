package ec.edu.mapsalud.dto

import ec.edu.mapsalud.enum.BloodType

data class Paciente(
    val info: UsuarioInfo = UsuarioInfo(),
    val tipoSangre: BloodType = BloodType.DESCONOCIDO,
    val contactoEmergencia: String = ""
)