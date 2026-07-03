package ec.edu.mapsalud.usercases.citasUC

import ec.edu.mapsalud.dto.AppointmentDtoRemote
import ec.edu.mapsalud.remote.impl.CitaRepositoryImpl

class SaveAppointmentUC(private val repo: CitaRepositoryImpl) {
    suspend fun invoke(appointment: AppointmentDtoRemote): Result<Unit> {
        return repo.saveAppointment(appointment)
    }
}