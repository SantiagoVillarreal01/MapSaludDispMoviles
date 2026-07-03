package ec.edu.mapsalud.usercases.consultoriosUC

import ec.edu.mapsalud.dto.OfficeDtoRemote
import ec.edu.mapsalud.remote.inter.ConsultorioRepository

class GetOfficeByIdUC(private val repository: ConsultorioRepository) {
    suspend operator fun invoke(idOffice: String): Result<OfficeDtoRemote?> {
        return repository.getOfficeById(idOffice)
    }
}