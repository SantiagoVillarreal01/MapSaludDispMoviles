package ec.edu.mapsalud.remote.impl

import com.google.firebase.firestore.FirebaseFirestore
import ec.edu.mapsalud.dto.Medico
import ec.edu.mapsalud.dto.OfficeDtoRemote
import ec.edu.mapsalud.remote.inter.OfficeRemote
import kotlinx.coroutines.tasks.await

class OfficeRemoteImpl : OfficeRemote {

    private val db = FirebaseFirestore.getInstance()

    override suspend fun getOfficesByCenterAndSpecialty(idCenter: String, specialty: String): Result<List<OfficeDtoRemote>> {
        return runCatching {
            val snapshot = db.collection("consultorios")
                .whereEqualTo("idCenter", idCenter)
                .whereEqualTo("specialty", specialty)
                .get()
                .await()

            snapshot.documents.map { doc ->
                doc.toObject(OfficeDtoRemote::class.java)!!.copy(id = doc.id)
            }
        }
    }

    override suspend fun getDoctorById(idDoctor: String): Result<Medico?> {
        return runCatching {
            val snapshot = db.collection("usuarios").document(idDoctor).get().await()
            if (snapshot.exists()) {
                val medico = snapshot.toObject(Medico::class.java)
                medico?.copy(
                    info = medico.info.copy(id = snapshot.id)
                )
            } else null
        }
    }
}