package ec.edu.mapsalud.usercases.consultoriosUC

import ec.edu.mapsalud.remote.impl.ConsultorioRepositoryImpl

class UpdateOfficeHorariesUC(val repository: ConsultorioRepositoryImpl) {
    suspend fun invoke(
        idOffice: String,
        availableDays: List<String>,
        openingTime: String,
        closingTime: String
    ): Result<Unit> {
        return repository.updateOfficeHoraries(idOffice, availableDays, openingTime, closingTime)
    }
}