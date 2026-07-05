package ec.edu.mapsalud.usercases.comentriosUC

import ec.edu.mapsalud.dto.ComentarioDtoRemote
import ec.edu.mapsalud.remote.impl.ComentarioRepositoryImpl

class SaveCommentUC(val repository: ComentarioRepositoryImpl) {
    suspend fun invoke(comment: ComentarioDtoRemote): Result<ComentarioDtoRemote> {
        return repository.saveComment(comment)
    }
}