package ec.edu.mapsalud.remote.impl

import com.google.firebase.firestore.FirebaseFirestore
import ec.edu.mapsalud.remote.inter.UsuariosRepository
import ec.edu.mapsalud.dto.Medico
import ec.edu.mapsalud.dto.Paciente
import kotlinx.coroutines.tasks.await

class UsuariosRepositoryImpl : UsuariosRepository {
    private val db = FirebaseFirestore.getInstance()

    override suspend fun getPacienteById(idUser: String): Result<Paciente?> = runCatching {
        val snapshot = db.collection("usuarios").document(idUser).get().await()
        if (snapshot.exists()) {
            snapshot.toObject(Paciente::class.java)?.copy(
                info = snapshot.toObject(Paciente::class.java)!!.info.copy(id = snapshot.id)
            )
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


    override suspend fun getPacienteByCedula(cedula: String): Result<Paciente?> = runCatching {
        val snapshot = db.collection("usuarios")
            .whereEqualTo("info.cedula", cedula)
            .get()
            .await()

        if (!snapshot.isEmpty) {
            val doc = snapshot.documents.first()
            doc.toObject(Paciente::class.java)?.copy(
                info = doc.toObject(Paciente::class.java)!!.info.copy(id = doc.id)
            )
        } else null
    }
}