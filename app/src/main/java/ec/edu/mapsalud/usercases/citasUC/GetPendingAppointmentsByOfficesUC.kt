package ec.edu.mapsalud.usercases.citasUC

import ec.edu.mapsalud.dto.CitaDtoRemote
import ec.edu.mapsalud.remote.impl.CitaRepositoryImpl
import ec.edu.mapsalud.remote.inter.CitaRepository
import kotlinx.coroutines.flow.Flow

class GetPendingAppointmentsByOfficesUC(private val repo: CitaRepository) {
    fun invoke(officeIds: List<String>): Flow<Result<List<CitaDtoRemote>>> {
        return repo.listenPendingAppointmentsByOffices(officeIds)
    }
}