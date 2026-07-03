package ec.edu.mapsalud.usercases.centrosUC

import ec.edu.mapsalud.dto.MedicalCenterDtoRemote
import ec.edu.mapsalud.remote.impl.CentroMedicoRepositoryImpl

class GetCentersFilteredUC(val repository: CentroMedicoRepositoryImpl) {
    suspend fun invoke(
        userLat: Double,
        userLon: Double,
        type: String?,
        specialty: String?,
        radiusInMeters: Double
    ): Result<List<Pair<MedicalCenterDtoRemote, Float>>> {
        return repository.getCentersFiltered(userLat, userLon, type, specialty, radiusInMeters)
    }
}