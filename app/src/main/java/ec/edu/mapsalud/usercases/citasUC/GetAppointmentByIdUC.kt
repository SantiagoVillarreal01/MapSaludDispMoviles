package ec.edu.mapsalud.usercases.citasUC

import ec.edu.mapsalud.dto.CitaDtoRemote
import ec.edu.mapsalud.remote.impl.CitaRepositoryImpl

class GetAppointmentByIdUC(private val repo: CitaRepositoryImpl) {
    suspend fun invoke(appointmentId: String): Result<CitaDtoRemote?> {
        return repo.getAppointmentById(appointmentId)
    }
}