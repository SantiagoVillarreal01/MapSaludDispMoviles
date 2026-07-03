package ec.edu.mapsalud.usercases.usuariosUC

import ec.edu.mapsalud.dto.Paciente
import ec.edu.mapsalud.remote.impl.UsuariosRepositoryImpl

class GetPacienteByIdUC(val repository: UsuariosRepositoryImpl) {
    suspend fun invoke(idUser: String): Result<Paciente?> {
        return repository.getPacienteById(idUser)
    }
}