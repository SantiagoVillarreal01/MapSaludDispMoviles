package ec.edu.mapsalud.remote.inter

import ec.edu.mapsalud.dto.ComentarioDtoRemote

interface ComentarioRepository {
    suspend fun saveComment(comment: ComentarioDtoRemote): Result<ComentarioDtoRemote>
    suspend fun getCommentsByCenter(idCenter: String): Result<List<ComentarioDtoRemote>>
}