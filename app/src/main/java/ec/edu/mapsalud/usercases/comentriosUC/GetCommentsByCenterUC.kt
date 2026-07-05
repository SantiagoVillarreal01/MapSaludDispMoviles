package ec.edu.mapsalud.usercases.comentriosUC

import ec.edu.mapsalud.dto.ComentarioDtoRemote
import ec.edu.mapsalud.remote.impl.ComentarioRepositoryImpl

class GetCommentsByCenterUC(val repository: ComentarioRepositoryImpl) {
    suspend fun invoke(idCenter: String): Result<List<ComentarioDtoRemote>> {
        return repository.getCommentsByCenter(idCenter)
    }
}