package ec.edu.mapsalud.remote.inter

import ec.edu.mapsalud.dto.CentroMedicoDtoRemote

interface CentroMedicoRepository {
    suspend fun getAllCenters(): Result<List<CentroMedicoDtoRemote>>
    suspend fun getCenterById(id: String): Result<CentroMedicoDtoRemote?>

    suspend fun getCentersFiltered(
        userLat: Double,
        userLon: Double,
        type: String?, // "Público", "Privado" o null para todos
        specialty: String?, // Especialidad o null para todas
        radiusInMeters: Double = 1000.0
    ): Result<List<Pair<CentroMedicoDtoRemote, Float>>>

    suspend fun addSpecialtyToCenter(idCenter: String, specialty: String): Result<Unit>
}