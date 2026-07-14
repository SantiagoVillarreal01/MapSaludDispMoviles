package ec.edu.mapsalud.usercases.citasUC

import ec.edu.mapsalud.dto.CitaDetalle
import ec.edu.mapsalud.remote.impl.CitaRepositoryImpl
import ec.edu.mapsalud.remote.impl.ConsultorioRepositoryImpl
import ec.edu.mapsalud.remote.impl.UsuariosRepositoryImpl
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Locale

class FetchAppointmentsWithDetailsUC(
    private val citaRepo: CitaRepositoryImpl,
    private val consultorioRepo: ConsultorioRepositoryImpl,
    private val usuarioRepo: UsuariosRepositoryImpl
) {
    fun invoke(userId: String, status: String): Flow<Result<List<CitaDetalle>>> {

        return citaRepo.listenAppointmentsByUserAndStatus(userId, status).map { result ->

            result.mapCatching { appointments ->
                if (appointments.isEmpty()) return@mapCatching emptyList()

                coroutineScope {
                    val uniqueOfficeIds = appointments.map { it.idOffice }.distinct()

                    val officesMap = uniqueOfficeIds.map { officeId ->
                        async { officeId to consultorioRepo.getOfficeById(officeId).getOrNull() }
                    }.awaitAll().toMap()

                    val uniqueDoctorIds = officesMap.values.mapNotNull { it?.idDoctor }.distinct()

                    val doctorsMap = uniqueDoctorIds.map { doctorId ->
                        async { doctorId to usuarioRepo.getDoctorById(doctorId).getOrNull() }
                    }.awaitAll().toMap()

                    val detailsList = appointments.mapNotNull { appointment ->
                        val office = officesMap[appointment.idOffice]
                        val doctor = office?.let { doctorsMap[it.idDoctor] }

                        if (office != null && doctor != null) {
                            CitaDetalle(
                                appointment = appointment,
                                office = office,
                                doctor = doctor
                            )
                        } else {
                            null
                        }
                    }

                    val dateTimeFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

                    detailsList.sortedWith { o1, o2 ->
                        val dateTimeStr1 = "${o1.appointment.date} ${o1.appointment.time}"
                        val dateTimeStr2 = "${o2.appointment.date} ${o2.appointment.time}"

                        val date1 = try { dateTimeFormatter.parse(dateTimeStr1) } catch (e: Exception) { null }
                        val date2 = try { dateTimeFormatter.parse(dateTimeStr2) } catch (e: Exception) { null }

                        when {
                            date1 == null && date2 == null -> 0
                            date1 == null -> 1
                            date2 == null -> -1
                            else -> date2.compareTo(date1)
                        }
                    }
                }
            }
        }
    }
}