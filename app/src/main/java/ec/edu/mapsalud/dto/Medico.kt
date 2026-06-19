package ec.edu.mapsalud.dto

import ec.edu.mapsalud.enum.Specialty
data class Medico(
    val info: UsuarioInfo = UsuarioInfo(),
    val specialty: Specialty = Specialty.GENERAL,
    val anosExperiencia: Int = 0
)