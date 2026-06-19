package ec.edu.mapsalud.dto

import ec.edu.mapsalud.enum.CenterType

data class MedicalCenterDtoRemote(
    val id: String = "",
    val name: String = "",
    val address: String = "",
    val description: String = "",
    val type: String = CenterType.PUBLICO.name,
    val specialties: List<String> = emptyList(),
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)