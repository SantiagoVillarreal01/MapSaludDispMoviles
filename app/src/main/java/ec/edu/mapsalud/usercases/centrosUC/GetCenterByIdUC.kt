package ec.edu.mapsalud.usercases.centrosUC

import ec.edu.mapsalud.dto.CentroMedicoDtoRemote
import ec.edu.mapsalud.remote.impl.CentroMedicoRepositoryImpl

class GetCenterByIdUC(val repository: CentroMedicoRepositoryImpl) {
    suspend fun invoke(id: String): Result<CentroMedicoDtoRemote?> {
        return repository.getCenterById(id)
    }
}