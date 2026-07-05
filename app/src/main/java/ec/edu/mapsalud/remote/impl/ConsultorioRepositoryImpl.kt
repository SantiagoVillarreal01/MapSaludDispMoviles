package ec.edu.mapsalud.remote.impl

import com.google.firebase.firestore.FirebaseFirestore
import ec.edu.mapsalud.dto.ConsultorioDtoRemote
import ec.edu.mapsalud.remote.inter.ConsultorioRepository
import kotlinx.coroutines.tasks.await

class ConsultorioRepositoryImpl : ConsultorioRepository {

    private val db = FirebaseFirestore.getInstance()

    override fun generateNewOfficeId(): String {
        return db.collection("consultorios").document().id
    }

    override suspend fun getOfficeById(idOffice: String): Result<ConsultorioDtoRemote?> = runCatching {
        val snap = db.collection("consultorios").document(idOffice).get().await()
        snap.toObject(ConsultorioDtoRemote::class.java)?.copy(id = snap.id)
    }


    override suspend fun saveOffice(office: ConsultorioDtoRemote): Result<Unit> = runCatching {
        db.collection("consultorios").document(office.id).set(office).await()
        Unit
    }

    override suspend fun getOfficesByCenterAndSpecialty(idCenter: String, specialty: String): Result<List<ConsultorioDtoRemote>> {
        return runCatching {
            val snapshot = db.collection("consultorios")
                .whereEqualTo("idCenter", idCenter)
                .whereEqualTo("specialty", specialty)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                doc.toObject(ConsultorioDtoRemote::class.java)?.copy(id = doc.id)
            }
        }
    }

    override suspend fun getOfficesByDoctor(idDoctor: String): Result<List<ConsultorioDtoRemote>> = runCatching {
        val snapshot = db.collection("consultorios")
            .whereEqualTo("idDoctor", idDoctor)
            .get()
            .await()
        snapshot.documents.mapNotNull { doc ->
            doc.toObject(ConsultorioDtoRemote::class.java)?.copy(id = doc.id)
        }
    }

    override suspend fun updateOfficeHoraries(
        idOffice: String,
        availableDays: List<String>,
        openingTime: String,
        closingTime: String
    ): Result<Unit> = runCatching {
        val actualizaciones = mapOf(
            "availableDays" to availableDays,
            "openingTime" to openingTime,
            "closingTime" to closingTime
        )
        db.collection("consultorios").document(idOffice).update(actualizaciones).await()
        Unit
    }
}