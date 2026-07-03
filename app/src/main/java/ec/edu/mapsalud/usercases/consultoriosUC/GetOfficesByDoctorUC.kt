package ec.edu.mapsalud.usercases.consultoriosUC

import ec.edu.mapsalud.dto.OfficeDtoRemote
import ec.edu.mapsalud.remote.impl.ConsultorioRepositoryImpl

class GetOfficesByDoctorUC(val repository: ConsultorioRepositoryImpl) {
    suspend fun invoke(idDoctor: String): Result<List<OfficeDtoRemote>> {
        return repository.getOfficesByDoctor(idDoctor)
    }
}