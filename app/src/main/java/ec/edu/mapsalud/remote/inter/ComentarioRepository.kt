package ec.edu.mapsalud.remote.inter

import ec.edu.mapsalud.dto.CommentDtoRemote

interface ComentarioRepository {
    suspend fun saveComment(comment: CommentDtoRemote): Result<CommentDtoRemote>
    suspend fun getCommentsByCenter(idCenter: String): Result<List<CommentDtoRemote>>
}