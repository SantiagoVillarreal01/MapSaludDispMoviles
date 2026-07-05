package ec.edu.mapsalud.usercases.citasUC

import ec.edu.mapsalud.dto.Diagnostico
import ec.edu.mapsalud.remote.impl.CitaRepositoryImpl

class UpdateAppointmentDiagnosisUC(private val repo: CitaRepositoryImpl) {
    suspend fun invoke(appointmentId: String, diagnosis: Diagnostico): Result<Unit> {
        return repo.updateAppointmentDiagnosis(appointmentId, diagnosis)
    }
}