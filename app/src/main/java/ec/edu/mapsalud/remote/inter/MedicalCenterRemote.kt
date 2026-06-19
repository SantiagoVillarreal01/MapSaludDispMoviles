package ec.edu.mapsalud.remote.inter

import ec.edu.mapsalud.dto.MedicalCenterDtoRemote

interface MedicalCenterRemote {
    suspend fun getAllCenters(): Result<List<MedicalCenterDtoRemote>>
    suspend fun getCenterById(id: String): Result<MedicalCenterDtoRemote?>

    suspend fun getCentersFiltered(
        userLat: Double,
        userLon: Double,
        type: String?, // "Público", "Privado" o null para todos
        specialty: String?, // Especialidad o null para todas
        radiusInMeters: Double = 1000.0
    ): Result<List<Pair<MedicalCenterDtoRemote, Float>>>
}