package ec.edu.mapsalud.usercases.citasUC

import ec.edu.mapsalud.remote.impl.CitaRepositoryImpl

class UpdateAppointmentUC(private val repo: CitaRepositoryImpl) {
    suspend fun invoke(appointmentId: String, newDate: String, newTime: String): Result<Unit> {
        return repo.updateAppointment(appointmentId, newDate, newTime)
    }
}