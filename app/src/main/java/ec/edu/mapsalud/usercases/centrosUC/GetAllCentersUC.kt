package ec.edu.mapsalud.usercases.centrosUC

import ec.edu.mapsalud.dto.MedicalCenterDtoRemote
import ec.edu.mapsalud.remote.impl.CentroMedicoRepositoryImpl

class GetAllCentersUC(val repository: CentroMedicoRepositoryImpl) {
    suspend fun invoke(): Result<List<MedicalCenterDtoRemote>> {
        return repository.getAllCenters()
    }
}