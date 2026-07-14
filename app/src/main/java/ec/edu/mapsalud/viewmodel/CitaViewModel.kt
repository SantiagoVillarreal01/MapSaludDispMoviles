package ec.edu.mapsalud.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ec.edu.mapsalud.dto.CitaDetalle
import ec.edu.mapsalud.dto.CitaDtoRemote
import ec.edu.mapsalud.dto.Diagnostico
import ec.edu.mapsalud.usercases.citasUC.CancelAppointmentUC
import ec.edu.mapsalud.usercases.citasUC.CheckIsSlotTakenUC
import ec.edu.mapsalud.usercases.citasUC.FetchAppointmentsWithDetailsUC
import ec.edu.mapsalud.usercases.citasUC.GetAppointmentByIdUC
import ec.edu.mapsalud.usercases.citasUC.GetCompletedAppointmentsByDoctorAndPatientUC
import ec.edu.mapsalud.usercases.citasUC.GetCompletedAppointmentsByDoctorUC
import ec.edu.mapsalud.usercases.citasUC.GetPendingAppointmentsByOfficesUC
import ec.edu.mapsalud.usercases.citasUC.SaveAppointmentUC
import ec.edu.mapsalud.usercases.citasUC.UpdateAppointmentDiagnosisUC
import ec.edu.mapsalud.usercases.citasUC.UpdateAppointmentStatusUC
import ec.edu.mapsalud.usercases.citasUC.UpdateAppointmentUC
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class CitaViewModel : ViewModel() {

    private val _isSlotTaken = MutableLiveData<Boolean>()
    val isSlotTaken: LiveData<Boolean> get() = _isSlotTaken

    private val _appointmentsDetails = MutableLiveData<List<CitaDetalle>>()
    val appointmentsDetails: LiveData<List<CitaDetalle>> get() = _appointmentsDetails
    private var diagnosticsListeningJob: Job? = null

    private val _appointmentsRawList = MutableLiveData<List<CitaDtoRemote>>()
    val appointmentsRawList: LiveData<List<CitaDtoRemote>> get() = _appointmentsRawList

    private var appointmentsListeningJob: Job? = null

    private val _selectedAppointment = MutableLiveData<CitaDtoRemote?>()
    val selectedAppointment: LiveData<CitaDtoRemote?> get() = _selectedAppointment

    private val _operationSuccess = MutableLiveData<Boolean>()
    val operationSuccess: LiveData<Boolean> get() = _operationSuccess

    fun verificarDisponibilidad(idOffice: String, date: String, time: String, checkUC: CheckIsSlotTakenUC) {
        viewModelScope.launch {
            val resultado = checkUC.invoke(idOffice, date, time).getOrDefault(false)
            _isSlotTaken.value = resultado
        }
    }

    fun agendarCita(appointment: CitaDtoRemote, saveUC: SaveAppointmentUC) {
        viewModelScope.launch {
            val exito = saveUC.invoke(appointment).isSuccess
            _operationSuccess.value = exito
        }
    }

    fun reprogramarCita(appointmentId: String, newDate: String, newTime: String, updateUC: UpdateAppointmentUC) {
        viewModelScope.launch {
            val exito = updateUC.invoke(appointmentId, newDate, newTime).isSuccess
            _operationSuccess.value = exito
        }
    }

    fun cancelarCita(appointmentId: String, cancelUC: CancelAppointmentUC) {
        viewModelScope.launch {
            val exito = cancelUC.invoke(appointmentId).isSuccess
            _operationSuccess.value = exito
        }
    }

    fun obtenerCitaPorId(appointmentId: String, getByIdUC: GetAppointmentByIdUC) {
        viewModelScope.launch {
            val resultado = getByIdUC.invoke(appointmentId).getOrNull()
            _selectedAppointment.value = resultado
        }
    }

    fun cambiarEstadoCita(appointmentId: String, status: String, updateStatusUC: UpdateAppointmentStatusUC) {
        viewModelScope.launch {
            val exito = updateStatusUC.invoke(appointmentId, status).isSuccess
            _operationSuccess.value = exito
        }
    }

    fun finalizarCitaConDiagnostico(appointmentId: String, diagnosis: Diagnostico, updateDiagnosisUC: UpdateAppointmentDiagnosisUC) {
        viewModelScope.launch {
            val exito = updateDiagnosisUC.invoke(appointmentId, diagnosis).isSuccess
            _operationSuccess.value = exito
        }
    }

    fun cargarCitasPendientesPorConsultorios(
        officeIds: List<String>,
        getByOfficesUC: GetPendingAppointmentsByOfficesUC
    ) {
        appointmentsListeningJob?.cancel()

        appointmentsListeningJob = viewModelScope.launch {
            getByOfficesUC.invoke(officeIds).collect { result ->
                val resultado = result.getOrNull()
                _appointmentsRawList.value = resultado ?: emptyList()
            }
        }
    }

    fun cargarCitasCompletadasPorDoctor(idDoctor: String, getByDoctorUC: GetCompletedAppointmentsByDoctorUC) {
        viewModelScope.launch {
            val resultado = getByDoctorUC.invoke(idDoctor).getOrNull()
            _appointmentsRawList.value = resultado ?: emptyList()
        }
    }

    fun cargarHistorialCompartido(idDoctor: String, idUser: String, getByDoctorAndPatientUC: GetCompletedAppointmentsByDoctorAndPatientUC) {
        viewModelScope.launch {
            val resultado = getByDoctorAndPatientUC.invoke(idDoctor, idUser).getOrNull()
            _appointmentsRawList.value = resultado ?: emptyList()
        }
    }

    fun cargarCitasConDetalles(
        userId: String,
        status: String,
        fetchDetailsUC: FetchAppointmentsWithDetailsUC
    ) {
        diagnosticsListeningJob?.cancel()

        diagnosticsListeningJob = viewModelScope.launch {
            fetchDetailsUC.invoke(userId, status).collect { result ->
                val listaConDetalles = result.getOrNull() ?: emptyList()
                _appointmentsDetails.value = listaConDetalles
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        appointmentsListeningJob?.cancel()
        diagnosticsListeningJob?.cancel()
    }
}