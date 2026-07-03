package ec.edu.mapsalud.usercases.comentriosUC

import ec.edu.mapsalud.dto.CommentDtoRemote
import ec.edu.mapsalud.remote.impl.ComentarioRepositoryImpl

class GetCommentsByCenterUC(val repository: ComentarioRepositoryImpl) {
    suspend fun invoke(idCenter: String): Result<List<CommentDtoRemote>> {
        return repository.getCommentsByCenter(idCenter)
    }
}