package ec.edu.mapsalud.usercases.consultoriosUC

import ec.edu.mapsalud.dto.ConsultorioDtoRemote
import ec.edu.mapsalud.remote.impl.ConsultorioRepositoryImpl

class SaveOfficeUC(val repository: ConsultorioRepositoryImpl) {
    suspend fun invoke(office: ConsultorioDtoRemote): Result<Unit> {
        return repository.saveOffice(office)
    }
}