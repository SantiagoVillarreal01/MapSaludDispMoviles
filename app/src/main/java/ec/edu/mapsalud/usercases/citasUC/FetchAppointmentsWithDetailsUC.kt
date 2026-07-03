package ec.edu.mapsalud.usercases.citasUC

import ec.edu.mapsalud.dto.AppointmentDetail
import ec.edu.mapsalud.remote.impl.CitaRepositoryImpl
import ec.edu.mapsalud.remote.impl.ConsultorioRepositoryImpl
import ec.edu.mapsalud.remote.impl.UsuariosRepositoryImpl
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class FetchAppointmentsWithDetailsUC(
    private val citaRepo: CitaRepositoryImpl,
    private val consultorioRepo: ConsultorioRepositoryImpl,
    private val usuarioRepo: UsuariosRepositoryImpl
) {
    suspend fun invoke(userId: String, status: String): Result<List<AppointmentDetail>> = runCatching {
        val appointments = citaRepo.fetchAppointmentsRaw(userId, status).getOrThrow()
        if (appointments.isEmpty()) return@runCatching emptyList()

        coroutineScope {
            val uniqueOfficeIds = appointments.map { it.idOffice }.distinct()

            val officesMap = uniqueOfficeIds.map { officeId ->
                async { officeId to consultorioRepo.getOfficeById(officeId).getOrNull() }
            }.awaitAll().toMap() // Devuelve un Map<String, ConsultorioDto?>

            val uniqueDoctorIds = officesMap.values.mapNotNull { it?.idDoctor }.distinct()

            val doctorsMap = uniqueDoctorIds.map { doctorId ->
                async { doctorId to usuarioRepo.getDoctorById(doctorId).getOrNull() }
            }.awaitAll().toMap() // Devuelve un Map<String, DoctorDto?>

            appointments.mapNotNull { appointment ->
                val office = officesMap[appointment.idOffice]
                val doctor = office?.let { doctorsMap[it.idDoctor] }

                if (office != null && doctor != null) {
                    AppointmentDetail(
                        appointment = appointment,
                        office = office,
                        doctor = doctor
                    )
                } else {
                    null
                }
            }
        }
    }
}