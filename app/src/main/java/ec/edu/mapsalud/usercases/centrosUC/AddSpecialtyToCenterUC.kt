package ec.edu.mapsalud.usercases.centrosUC

import ec.edu.mapsalud.remote.impl.CentroMedicoRepositoryImpl

class AddSpecialtyToCenterUC(val repository: CentroMedicoRepositoryImpl) {
    suspend fun invoke(idCenter: String, specialty: String): Result<Unit> {
        return repository.addSpecialtyToCenter(idCenter, specialty)
    }
}