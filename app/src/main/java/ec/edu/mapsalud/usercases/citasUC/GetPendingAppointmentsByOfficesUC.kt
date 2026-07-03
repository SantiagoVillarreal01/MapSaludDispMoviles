package ec.edu.mapsalud.usercases.citasUC

import ec.edu.mapsalud.dto.AppointmentDtoRemote
import ec.edu.mapsalud.remote.impl.CitaRepositoryImpl

class GetPendingAppointmentsByOfficesUC(private val repo: CitaRepositoryImpl) {
    suspend fun invoke(officeIds: List<String>): Result<List<AppointmentDtoRemote>> {
        return repo.getPendingAppointmentsByOffices(officeIds)
    }
}