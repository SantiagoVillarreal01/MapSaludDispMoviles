package ec.edu.mapsalud.usercases.usuariosUC

import ec.edu.mapsalud.dto.Medico
import ec.edu.mapsalud.remote.impl.UsuariosRepositoryImpl

class GetDoctorByIdUC(val repository: UsuariosRepositoryImpl) {
    suspend fun invoke(idDoctor: String): Result<Medico?> {
        return repository.getDoctorById(idDoctor)
    }
}