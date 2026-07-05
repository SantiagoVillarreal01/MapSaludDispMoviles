package ec.edu.mapsalud.usercases.citasUC

import ec.edu.mapsalud.dto.CitaDtoRemote
import ec.edu.mapsalud.remote.impl.CitaRepositoryImpl

class GetCompletedAppointmentsByDoctorUC(private val repo: CitaRepositoryImpl) {
    suspend fun invoke(idDoctor: String): Result<List<CitaDtoRemote>> {
        return repo.getCompletedAppointmentsByDoctor(idDoctor)
    }
}