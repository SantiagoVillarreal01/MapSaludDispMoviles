package ec.edu.mapsalud.remote.impl

import com.google.firebase.firestore.FirebaseFirestore
import ec.edu.mapsalud.dto.AppointmentDetail
import ec.edu.mapsalud.dto.AppointmentDtoRemote
import ec.edu.mapsalud.dto.MedicalCenterDtoRemote
import ec.edu.mapsalud.dto.Medico
import ec.edu.mapsalud.dto.OfficeDtoRemote
import ec.edu.mapsalud.dto.Paciente
import ec.edu.mapsalud.remote.inter.AppointmentRemote
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await


class AppointmentRemoteImpl : AppointmentRemote {
    private val db = FirebaseFirestore.getInstance()

    override suspend fun getOfficeById(idOffice: String): Result<OfficeDtoRemote?> = runCatching {
        val snap = db.collection("consultorios").document(idOffice).get().await()
        snap.toObject(OfficeDtoRemote::class.java)?.copy(id = snap.id)
    }

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

    override suspend fun saveAppointment(appointment: AppointmentDtoRemote): Result<Unit> =
        runCatching {
            // Al crear un documento sin ID, Firestore genera uno automáticamente
            val docRef = db.collection("citas").document()
            val appointmentToSave = appointment.copy(id = docRef.id)
            docRef.set(appointmentToSave).await()
        }

    override suspend fun getPacienteById(idUser: String): Result<Paciente?> = runCatching {
        val snapshot = db.collection("usuarios").document(idUser).get().await()
        if (snapshot.exists()) {
            snapshot.toObject(Paciente::class.java)?.copy(
                info = snapshot.toObject(Paciente::class.java)!!.info.copy(id = snapshot.id)
            )
        } else null
    }

    override suspend fun getCenterById(idCenter: String): Result<MedicalCenterDtoRemote?> = runCatching {
        val snapshot = db.collection("centros_medicos").document(idCenter).get().await()
        if (snapshot.exists()) {
            snapshot.toObject(MedicalCenterDtoRemote::class.java)?.copy(id = snapshot.id)
        } else null
    }

    override suspend fun getDoctorById(idDoctor: String): Result<Medico?> = runCatching {
        val snapshot = db.collection("usuarios").document(idDoctor).get().await()
        if (snapshot.exists()) {
            val medico = snapshot.toObject(Medico::class.java)
            medico?.copy(
                info = medico.info.copy(id = snapshot.id)
            )
        } else null
    }

    suspend fun getPendingAppointmentsWithDetails(userId: String): Result<List<AppointmentDetail>> {
        return fetchAppointmentsWithDetails(userId, "Pendiente")
    }

    suspend fun getCompletedAppointmentsWithDiagnosis(userId: String): Result<List<AppointmentDetail>> {
        return fetchAppointmentsWithDetails(userId, "Completada")
    }
    override suspend fun fetchAppointmentsWithDetails(userId: String, status: String): Result<List<AppointmentDetail>> = runCatching {
        val appointmentsSnapshot = db.collection("citas")
            .whereEqualTo("idUser", userId)
            .whereEqualTo("status", status)
            .get()
            .await()

        val appointments = appointmentsSnapshot.documents.mapNotNull { doc ->
            doc.toObject(AppointmentDtoRemote::class.java)?.copy(id = doc.id)
        }

        coroutineScope {
            appointments.map { appointment ->
                async {
                    val officeSnap = db.collection("consultorios").document(appointment.idOffice).get().await()
                    val office = officeSnap.toObject(OfficeDtoRemote::class.java)?.copy(id = officeSnap.id)

                    val doctorSnap = if (office != null) {
                        db.collection("usuarios").document(office.idDoctor).get().await()
                    } else null

                    val doctor = doctorSnap?.toObject(Medico::class.java)
                    val doctorConId = doctor?.copy(
                        info = doctor.info.copy(id = doctorSnap.id)
                    )

                    if (office != null && doctorConId != null) {
                        AppointmentDetail(appointment, doctorConId, office)
                    } else null
                }
            }.awaitAll().filterNotNull()
        }
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
}