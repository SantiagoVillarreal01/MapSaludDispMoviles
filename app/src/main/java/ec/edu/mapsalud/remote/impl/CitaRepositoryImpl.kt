package ec.edu.mapsalud.remote.impl

import com.google.firebase.firestore.FirebaseFirestore
import ec.edu.mapsalud.dto.CitaDtoRemote
import ec.edu.mapsalud.dto.Diagnostico
import ec.edu.mapsalud.remote.inter.CitaRepository
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow


class CitaRepositoryImpl : CitaRepository {
    private val db = FirebaseFirestore.getInstance()

    override suspend fun checkIsSlotTaken(
        idOffice: String,
        date: String,
        time: String
    ): Result<Boolean> = runCatching {
        val snapshot = db.collection("citas")
            .whereEqualTo("idOffice", idOffice)
            .whereEqualTo("date", date)
            .whereEqualTo("time", time)
            .whereNotEqualTo("status", "Cancelada")
            .get()
            .await()
        !snapshot.isEmpty
    }

    override suspend fun saveAppointment(appointment: CitaDtoRemote): Result<Unit> =
        runCatching {
            val docRef = db.collection("citas").document()
            val appointmentToSave = appointment.copy(id = docRef.id)
            docRef.set(appointmentToSave).await()
        }

    override suspend fun updateAppointment(appointmentId: String, newDate: String, newTime: String): Result<Unit> = runCatching {
        db.collection("citas").document(appointmentId)
            .update(
                mapOf(
                    "date" to newDate,
                    "time" to newTime
                )
            ).await()
    }

    override suspend fun cancelAppointment(appointmentId: String): Result<Unit> = runCatching {
        db.collection("citas").document(appointmentId)
            .update("status", "Cancelada")
            .await()
    }

    override suspend fun getPendingAppointmentsByOffices(officeIds: List<String>): Result<List<CitaDtoRemote>> = runCatching {
        if (officeIds.isEmpty()) return@runCatching emptyList()

        val snapshot = db.collection("citas")
            .whereIn("idOffice", officeIds)
            .whereEqualTo("status", "Pendiente")
            .get()
            .await()
        snapshot.toObjects(CitaDtoRemote::class.java)
    }

    override suspend fun getAppointmentById(appointmentId: String): Result<CitaDtoRemote?> = runCatching {
        val snap = db.collection("citas").document(appointmentId).get().await()
        if (snap.exists()) {
            snap.toObject(CitaDtoRemote::class.java)?.copy(id = snap.id)
        } else null
    }

    override suspend fun updateAppointmentStatus(appointmentId: String, status: String): Result<Unit> = runCatching {
        db.collection("citas").document(appointmentId)
            .update("status", status)
            .await()
        Unit
    }

    override suspend fun getCompletedAppointmentsByDoctor(idDoctor: String): Result<List<CitaDtoRemote>> = runCatching {
        val snapshot = db.collection("citas")
            .whereEqualTo("idDoctor", idDoctor)
            .whereEqualTo("status", "Completada")
            .get()
            .await()
        snapshot.documents.mapNotNull { doc ->
            doc.toObject(CitaDtoRemote::class.java)?.copy(id = doc.id)
        }
    }

    override suspend fun getCompletedAppointmentsByDoctorAndPatient(idDoctor: String, idUser: String): Result<List<CitaDtoRemote>> = runCatching {
        val snapshot = db.collection("citas")
            .whereEqualTo("idUser", idUser)
            .whereEqualTo("idDoctor", idDoctor)
            .whereEqualTo("status", "Completada")
            .get()
            .await()
        snapshot.documents.mapNotNull { doc ->
            doc.toObject(CitaDtoRemote::class.java)?.copy(id = doc.id)
        }
    }

    override suspend fun updateAppointmentDiagnosis(appointmentId: String, diagnosis: Diagnostico): Result<Unit> = runCatching {
        db.collection("citas").document(appointmentId)
            .update(
                mapOf(
                    "diagnosis" to diagnosis,
                    "status" to "Completada"
                )
            ).await()
        Unit
    }

    override suspend fun fetchAppointmentsRaw(userId: String, status: String): Result<List<CitaDtoRemote>> = runCatching {
        val appointmentsSnapshot = db.collection("citas")
            .whereEqualTo("idUser", userId)
            .whereEqualTo("status", status)
            .get()
            .await()

        appointmentsSnapshot.documents.mapNotNull { doc ->
            doc.toObject(CitaDtoRemote::class.java)?.copy(id = doc.id)
        }
    }

    override fun listenPendingAppointmentsByOffices(officeIds: List<String>): Flow<Result<List<CitaDtoRemote>>> = callbackFlow {
        if (officeIds.isEmpty()) {
            trySend(Result.success(emptyList()))
            close()
            return@callbackFlow
        }

        val query = db.collection("citas")
            .whereIn("idOffice", officeIds)
            .whereEqualTo("status", "Pendiente")

        val listenerRegistration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(Result.failure(error))
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val appointments = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(CitaDtoRemote::class.java)?.copy(id = doc.id)
                }
                trySend(Result.success(appointments))
            }
        }
        awaitClose {
            listenerRegistration.remove()
        }
    }

    override fun listenAppointmentsByUserAndStatus(userId: String, status: String): Flow<Result<List<CitaDtoRemote>>> = callbackFlow {
        val query = db.collection("citas")
            .whereEqualTo("idUser", userId)
            .whereEqualTo("status", status)

        val listenerRegistration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(Result.failure(error))
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val appointments = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(CitaDtoRemote::class.java)?.copy(id = doc.id)
                }
                trySend(Result.success(appointments))
            }
        }
        awaitClose {
            listenerRegistration.remove()
        }
    }
}