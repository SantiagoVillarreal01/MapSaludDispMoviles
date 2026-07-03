package ec.edu.mapsalud.usercases.citasUC

import ec.edu.mapsalud.dto.AppointmentDtoRemote
import ec.edu.mapsalud.remote.impl.CitaRepositoryImpl

class GetCompletedAppointmentsByDoctorUC(private val repo: CitaRepositoryImpl) {
    suspend fun invoke(idDoctor: String): Result<List<AppointmentDtoRemote>> {
        return repo.getCompletedAppointmentsByDoctor(idDoctor)
    }
}