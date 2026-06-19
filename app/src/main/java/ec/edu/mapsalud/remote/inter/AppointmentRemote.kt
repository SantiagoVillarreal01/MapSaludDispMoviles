package ec.edu.mapsalud.remote.inter

import ec.edu.mapsalud.dto.AppointmentDetail
import ec.edu.mapsalud.dto.AppointmentDtoRemote
import ec.edu.mapsalud.dto.OfficeDtoRemote

interface AppointmentRemote {
    suspend fun getOfficeById(idOffice: String): Result<OfficeDtoRemote?>
    suspend fun checkIsSlotTaken(idOffice: String, date: String, time: String): Result<Boolean>
    suspend fun saveAppointment(appointment: AppointmentDtoRemote): Result<Unit>
    suspend fun fetchAppointmentsWithDetails(userId: String, status: String): Result<List<AppointmentDetail>>
    suspend fun updateAppointment(appointmentId: String, newDate: String, newTime: String): Result<Unit>
    suspend fun cancelAppointment(appointmentId: String): Result<Unit>

}