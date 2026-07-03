package ec.edu.mapsalud.usercases.citasUC

import ec.edu.mapsalud.remote.impl.CitaRepositoryImpl

class CancelAppointmentUC(private val repo: CitaRepositoryImpl) {
    suspend fun invoke(appointmentId: String): Result<Unit> {
        return repo.cancelAppointment(appointmentId)
    }
}