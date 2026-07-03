package ec.edu.mapsalud.usercases.citasUC

import ec.edu.mapsalud.dto.AppointmentDtoRemote
import ec.edu.mapsalud.remote.impl.CitaRepositoryImpl

class GetAppointmentByIdUC(private val repo: CitaRepositoryImpl) {
    suspend fun invoke(appointmentId: String): Result<AppointmentDtoRemote?> {
        return repo.getAppointmentById(appointmentId)
    }
}