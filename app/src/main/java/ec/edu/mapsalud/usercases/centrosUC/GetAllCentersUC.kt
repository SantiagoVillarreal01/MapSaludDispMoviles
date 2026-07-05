package ec.edu.mapsalud.usercases.centrosUC

import ec.edu.mapsalud.dto.CentroMedicoDtoRemote
import ec.edu.mapsalud.remote.impl.CentroMedicoRepositoryImpl

class GetAllCentersUC(val repository: CentroMedicoRepositoryImpl) {
    suspend fun invoke(): Result<List<CentroMedicoDtoRemote>> {
        return repository.getAllCenters()
    }
}