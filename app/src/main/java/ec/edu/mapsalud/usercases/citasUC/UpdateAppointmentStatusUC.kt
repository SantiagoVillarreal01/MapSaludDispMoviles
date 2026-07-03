package ec.edu.mapsalud.usercases.citasUC

import ec.edu.mapsalud.remote.impl.CitaRepositoryImpl

class UpdateAppointmentStatusUC(private val repo: CitaRepositoryImpl) {
    suspend fun invoke(appointmentId: String, status: String): Result<Unit> {
        return repo.updateAppointmentStatus(appointmentId, status)
    }
}