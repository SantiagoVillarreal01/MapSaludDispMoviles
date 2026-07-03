package ec.edu.mapsalud.remote.inter

import ec.edu.mapsalud.dto.Medico
import ec.edu.mapsalud.dto.OfficeDtoRemote

interface ConsultorioRepository {

    fun generateNewOfficeId(): String
    suspend fun getOfficeById(idOffice: String): Result<OfficeDtoRemote?>
    suspend fun saveOffice(office: OfficeDtoRemote): Result<Unit>
    suspend fun getOfficesByCenterAndSpecialty(idCenter: String, specialty: String): Result<List<OfficeDtoRemote>>
    suspend fun getOfficesByDoctor(idDoctor: String): Result<List<OfficeDtoRemote>>
    suspend fun updateOfficeHoraries(
        idOffice: String,
        availableDays: List<String>,
        openingTime: String,
        closingTime: String
    ): Result<Unit>

}