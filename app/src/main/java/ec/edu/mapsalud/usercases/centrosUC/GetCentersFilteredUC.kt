package ec.edu.mapsalud.usercases.centrosUC

import ec.edu.mapsalud.dto.CentroMedicoDtoRemote
import ec.edu.mapsalud.remote.impl.CentroMedicoRepositoryImpl

class GetCentersFilteredUC(val repository: CentroMedicoRepositoryImpl) {
    suspend fun invoke(
        userLat: Double,
        userLon: Double,
        type: String?,
        specialty: String?,
        radiusInMeters: Double
    ): Result<List<Pair<CentroMedicoDtoRemote, Float>>> {
        return repository.getCentersFiltered(userLat, userLon, type, specialty, radiusInMeters)
    }
}