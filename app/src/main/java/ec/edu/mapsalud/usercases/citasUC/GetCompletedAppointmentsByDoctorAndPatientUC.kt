package ec.edu.mapsalud.usercases.citasUC

import ec.edu.mapsalud.dto.CitaDtoRemote
import ec.edu.mapsalud.remote.impl.CitaRepositoryImpl

class GetCompletedAppointmentsByDoctorAndPatientUC(private val repo: CitaRepositoryImpl) {
    suspend fun invoke(idDoctor: String, idUser: String): Result<List<CitaDtoRemote>> {
        return repo.getCompletedAppointmentsByDoctorAndPatient(idDoctor, idUser)
    }
}