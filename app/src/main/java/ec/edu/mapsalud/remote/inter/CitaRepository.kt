package ec.edu.mapsalud.remote.inter

import ec.edu.mapsalud.dto.AppointmentDetail
import ec.edu.mapsalud.dto.AppointmentDtoRemote
import ec.edu.mapsalud.dto.DiagnosisEmbedded
import ec.edu.mapsalud.dto.MedicalCenterDtoRemote
import ec.edu.mapsalud.dto.Medico
import ec.edu.mapsalud.dto.OfficeDtoRemote
import ec.edu.mapsalud.dto.Paciente

interface CitaRepository {
    suspend fun checkIsSlotTaken(idOffice: String, date: String, time: String): Result<Boolean>
    suspend fun saveAppointment(appointment: AppointmentDtoRemote): Result<Unit>
    suspend fun updateAppointment(appointmentId: String, newDate: String, newTime: String): Result<Unit>
    suspend fun cancelAppointment(appointmentId: String): Result<Unit>
    suspend fun getPendingAppointmentsByOffices(officeIds: List<String>): Result<List<AppointmentDtoRemote>>
    suspend fun getAppointmentById(appointmentId: String): Result<AppointmentDtoRemote?>
    suspend fun updateAppointmentStatus(appointmentId: String, status: String): Result<Unit>
    suspend fun getCompletedAppointmentsByDoctor(idDoctor: String): Result<List<AppointmentDtoRemote>>
    suspend fun getCompletedAppointmentsByDoctorAndPatient(idDoctor: String, idUser: String): Result<List<AppointmentDtoRemote>>

    suspend fun updateAppointmentDiagnosis(appointmentId: String, diagnosis: DiagnosisEmbedded): Result<Unit>

    suspend fun fetchAppointmentsRaw(userId: String, status: String): Result<List<AppointmentDtoRemote>>

}