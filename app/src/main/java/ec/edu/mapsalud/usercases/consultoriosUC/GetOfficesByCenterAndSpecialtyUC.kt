package ec.edu.mapsalud.usercases.consultoriosUC

import ec.edu.mapsalud.dto.OfficeDtoRemote
import ec.edu.mapsalud.remote.impl.ConsultorioRepositoryImpl

class GetOfficesByCenterAndSpecialtyUC(val repository: ConsultorioRepositoryImpl) {
    suspend fun invoke(idCenter: String, specialty: String): Result<List<OfficeDtoRemote>> {
        return repository.getOfficesByCenterAndSpecialty(idCenter, specialty)
    }
}