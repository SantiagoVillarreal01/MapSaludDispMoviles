package ec.edu.mapsalud.usercases.comentriosUC

import ec.edu.mapsalud.dto.CommentDtoRemote
import ec.edu.mapsalud.remote.impl.ComentarioRepositoryImpl

class SaveCommentUC(val repository: ComentarioRepositoryImpl) {
    suspend fun invoke(comment: CommentDtoRemote): Result<CommentDtoRemote> {
        return repository.saveComment(comment)
    }
}