package ec.edu.mapsalud.remote.inter

import ec.edu.mapsalud.dto.ConsultorioDtoRemote

interface ConsultorioRepository {

    fun generateNewOfficeId(): String
    suspend fun getOfficeById(idOffice: String): Result<ConsultorioDtoRemote?>
    suspend fun saveOffice(office: ConsultorioDtoRemote): Result<Unit>
    suspend fun getOfficesByCenterAndSpecialty(idCenter: String, specialty: String): Result<List<ConsultorioDtoRemote>>
    suspend fun getOfficesByDoctor(idDoctor: String): Result<List<ConsultorioDtoRemote>>
    suspend fun updateOfficeHoraries(
        idOffice: String,
        availableDays: List<String>,
        openingTime: String,
        closingTime: String
    ): Result<Unit>

}