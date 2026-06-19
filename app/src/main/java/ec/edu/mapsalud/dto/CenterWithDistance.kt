package ec.edu.mapsalud.dto

data class CenterWithDistance(
    val center: MedicalCenterDtoRemote,
    val distanceMeters: Float
)