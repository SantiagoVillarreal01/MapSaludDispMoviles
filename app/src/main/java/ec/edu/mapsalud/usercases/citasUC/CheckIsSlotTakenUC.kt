package ec.edu.mapsalud.usercases.citasUC

import ec.edu.mapsalud.remote.impl.CitaRepositoryImpl

class CheckIsSlotTakenUC(private val repo: CitaRepositoryImpl) {
    suspend fun invoke(idOffice: String, date: String, time: String): Result<Boolean> {
        return repo.checkIsSlotTaken(idOffice, date, time)
    }
}