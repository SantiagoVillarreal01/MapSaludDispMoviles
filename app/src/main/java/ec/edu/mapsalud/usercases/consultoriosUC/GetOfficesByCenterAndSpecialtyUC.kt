package ec.edu.mapsalud.usercases.consultoriosUC

import ec.edu.mapsalud.dto.ConsultorioDtoRemote
import ec.edu.mapsalud.remote.impl.ConsultorioRepositoryImpl

class GetOfficesByCenterAndSpecialtyUC(val repository: ConsultorioRepositoryImpl) {
    suspend fun invoke(idCenter: String, specialty: String): Result<List<ConsultorioDtoRemote>> {
        return repository.getOfficesByCenterAndSpecialty(idCenter, specialty)
    }
}