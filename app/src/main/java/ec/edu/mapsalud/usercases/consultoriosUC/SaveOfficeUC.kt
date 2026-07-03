package ec.edu.mapsalud.usercases.consultoriosUC

import ec.edu.mapsalud.dto.OfficeDtoRemote
import ec.edu.mapsalud.remote.impl.ConsultorioRepositoryImpl

class SaveOfficeUC(val repository: ConsultorioRepositoryImpl) {
    suspend fun invoke(office: OfficeDtoRemote): Result<Unit> {
        return repository.saveOffice(office)
    }
}