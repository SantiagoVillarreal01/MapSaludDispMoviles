package ec.edu.mapsalud.remote.inter

import ec.edu.mapsalud.dto.Medico
import ec.edu.mapsalud.dto.OfficeDtoRemote

interface OfficeRemote {

    suspend fun getOfficesByCenterAndSpecialty(idCenter: String, specialty: String): Result<List<OfficeDtoRemote>>
    suspend fun getDoctorById(idDoctor: String): Result<Medico?>
}