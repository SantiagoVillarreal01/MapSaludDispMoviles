package ec.edu.mapsalud.remote.impl

import com.google.firebase.firestore.FirebaseFirestore
import ec.edu.mapsalud.remote.inter.CentroMedicoRepository
import ec.edu.mapsalud.dto.CentroMedicoDtoRemote
import kotlinx.coroutines.tasks.await
import android.location.Location
import com.google.firebase.firestore.FieldValue

class CentroMedicoRepositoryImpl : CentroMedicoRepository {

    private val db = FirebaseFirestore.getInstance()

    override suspend fun getAllCenters(): Result<List<CentroMedicoDtoRemote>> {
        return runCatching {
            val snapshot = db.collection("centros_medicos").get().await()

            snapshot.documents.map { doc ->
                val centro = doc.toObject(CentroMedicoDtoRemote::class.java)!!
                centro.copy(id = doc.id)
            }
        }
    }

    override suspend fun getCenterById(id: String): Result<CentroMedicoDtoRemote?> {
        return runCatching {
            val snapshot = db.collection("centros_medicos").document(id).get().await()
            if (snapshot.exists()) {
                snapshot.toObject(CentroMedicoDtoRemote::class.java)?.copy(id = snapshot.id)
            } else null
        }
    }


    override suspend fun getCentersFiltered(
        userLat: Double,
        userLon: Double,
        type: String?,
        specialty: String?,
        radiusInMeters: Double
    ): Result<List<Pair<CentroMedicoDtoRemote, Float>>> = runCatching {

        val snapshot = db.collection("centros_medicos").get().await()

        val results = mutableListOf<Pair<CentroMedicoDtoRemote, Float>>()

        snapshot.documents.forEach { doc ->
            val centro = doc.toObject(CentroMedicoDtoRemote::class.java)?.copy(id = doc.id)

            if (centro != null) {
                val matchesType = type == null || type == "Todos" || centro.type.equals(type, ignoreCase = true)
                val matchesSpecialty = specialty == null || specialty == "Todas" || centro.specialties.contains(specialty)

                if (matchesType && matchesSpecialty) {
                    val distanceResults = FloatArray(1)
                    Location.distanceBetween(
                        userLat, userLon,
                        centro.latitude, centro.longitude,
                        distanceResults
                    )

                    val distance = distanceResults[0]

                    if (distance <= radiusInMeters) {
                        results.add(centro to distance)
                    }
                }
            }
        }
        results.sortBy { it.second }
        results
    }

    override suspend fun addSpecialtyToCenter(idCenter: String, specialty: String): Result<Unit> = runCatching {
        db.collection("centros_medicos").document(idCenter)
            .update("specialties", FieldValue.arrayUnion(specialty))
            .await()
        Unit
    }
}