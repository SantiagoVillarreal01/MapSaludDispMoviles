package ec.edu.mapsalud.usercases.usuariosUC

import ec.edu.mapsalud.dto.Paciente
import ec.edu.mapsalud.remote.impl.UsuariosRepositoryImpl

class GetPacienteByCedulaUC(val repository: UsuariosRepositoryImpl) {
    suspend fun invoke(cedula: String): Result<Paciente?> {
        return repository.getPacienteByCedula(cedula)
    }
}