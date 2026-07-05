package ec.edu.mapsalud.usercases.consultoriosUC

import ec.edu.mapsalud.dto.ConsultorioDtoRemote
import ec.edu.mapsalud.remote.impl.ConsultorioRepositoryImpl

class GetOfficesByDoctorUC(val repository: ConsultorioRepositoryImpl) {
    suspend fun invoke(idDoctor: String): Result<List<ConsultorioDtoRemote>> {
        return repository.getOfficesByDoctor(idDoctor)
    }
}