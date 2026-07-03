package ec.edu.mapsalud.remote.impl

import com.google.firebase.firestore.FirebaseFirestore
import ec.edu.mapsalud.dto.CommentDtoRemote
import ec.edu.mapsalud.remote.inter.ComentarioRepository
import kotlinx.coroutines.tasks.await


class ComentarioRepositoryImpl : ComentarioRepository {
    private val db = FirebaseFirestore.getInstance()

    override suspend fun saveComment(comment: CommentDtoRemote): Result<CommentDtoRemote> {
        return runCatching {
            val docRef = db.collection("comentarios").add(comment).await()
            comment.copy(id = docRef.id)
        }
    }

    override suspend fun getCommentsByCenter(idCenter: String): Result<List<CommentDtoRemote>> {
        return runCatching {
            val snapshot = db.collection("comentarios")
                .whereEqualTo("idCenter", idCenter)
                .get()
                .await()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(CommentDtoRemote::class.java)?.copy(id = doc.id)
            }
        }
    }
}