package ec.edu.mapsalud.remote.inter

import ec.edu.mapsalud.dto.CitaDtoRemote
import ec.edu.mapsalud.dto.Diagnostico
import kotlinx.coroutines.flow.Flow

interface CitaRepository {
    suspend fun checkIsSlotTaken(idOffice: String, date: String, time: String): Result<Boolean>
    suspend fun saveAppointment(appointment: CitaDtoRemote): Result<Unit>
    suspend fun updateAppointment(appointmentId: String, newDate: String, newTime: String): Result<Unit>
    suspend fun cancelAppointment(appointmentId: String): Result<Unit>
    suspend fun getPendingAppointmentsByOffices(officeIds: List<String>): Result<List<CitaDtoRemote>>
    suspend fun getAppointmentById(appointmentId: String): Result<CitaDtoRemote?>
    suspend fun updateAppointmentStatus(appointmentId: String, status: String): Result<Unit>
    suspend fun getCompletedAppointmentsByDoctor(idDoctor: String): Result<List<CitaDtoRemote>>
    suspend fun getCompletedAppointmentsByDoctorAndPatient(idDoctor: String, idUser: String): Result<List<CitaDtoRemote>>
    suspend fun updateAppointmentDiagnosis(appointmentId: String, diagnosis: Diagnostico): Result<Unit>
    suspend fun fetchAppointmentsRaw(userId: String, status: String): Result<List<CitaDtoRemote>>

    fun listenPendingAppointmentsByOffices(officeIds: List<String>): Flow<Result<List<CitaDtoRemote>>>

    // En CitaRepository.kt
    fun listenAppointmentsByUserAndStatus(userId: String, status: String): Flow<Result<List<CitaDtoRemote>>>
}