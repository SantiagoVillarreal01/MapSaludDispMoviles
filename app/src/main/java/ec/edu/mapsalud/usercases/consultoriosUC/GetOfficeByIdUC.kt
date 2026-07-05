package ec.edu.mapsalud.usercases.consultoriosUC

import ec.edu.mapsalud.dto.ConsultorioDtoRemote
import ec.edu.mapsalud.remote.inter.ConsultorioRepository

class GetOfficeByIdUC(private val repository: ConsultorioRepository) {
    suspend operator fun invoke(idOffice: String): Result<ConsultorioDtoRemote?> {
        return repository.getOfficeById(idOffice)
    }
}