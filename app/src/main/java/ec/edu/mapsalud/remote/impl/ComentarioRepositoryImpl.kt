package ec.edu.mapsalud.remote.impl

import com.google.firebase.firestore.FirebaseFirestore
import ec.edu.mapsalud.dto.ComentarioDtoRemote
import ec.edu.mapsalud.remote.inter.ComentarioRepository
import kotlinx.coroutines.tasks.await


class ComentarioRepositoryImpl : ComentarioRepository {
    private val db = FirebaseFirestore.getInstance()

    override suspend fun saveComment(comment: ComentarioDtoRemote): Result<ComentarioDtoRemote> {
        return runCatching {
            val docRef = db.collection("comentarios").add(comment).await()
            comment.copy(id = docRef.id)
        }
    }

    override suspend fun getCommentsByCenter(idCenter: String): Result<List<ComentarioDtoRemote>> {
        return runCatching {
            val snapshot = db.collection("comentarios")
                .whereEqualTo("idCenter", idCenter)
                .get()
                .await()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(ComentarioDtoRemote::class.java)?.copy(id = doc.id)
            }
        }
    }
}