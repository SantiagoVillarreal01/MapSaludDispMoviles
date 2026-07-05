package ec.edu.mapsalud.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ec.edu.mapsalud.dto.ConsultorioDtoRemote
import ec.edu.mapsalud.usercases.consultoriosUC.GetOfficeByIdUC
import ec.edu.mapsalud.usercases.consultoriosUC.GetOfficesByCenterAndSpecialtyUC
import ec.edu.mapsalud.usercases.consultoriosUC.GetOfficesByDoctorUC
import ec.edu.mapsalud.usercases.consultoriosUC.SaveOfficeUC
import ec.edu.mapsalud.usercases.consultoriosUC.UpdateOfficeHorariesUC
import kotlinx.coroutines.launch

class ConsultorioViewModel : ViewModel() {

    private var _selectedOffice = MutableLiveData<ConsultorioDtoRemote?>()
    val selectedOffice: LiveData<ConsultorioDtoRemote?> get() = _selectedOffice

    private var _officesList = MutableLiveData<List<ConsultorioDtoRemote>>()
    val officesList: LiveData<List<ConsultorioDtoRemote>> get() = _officesList

    private var _operationResult = MutableLiveData<Boolean>()
    val operationResult: LiveData<Boolean> get() = _operationResult

    fun obtenerConsultorioPorId(idOffice: String, getOfficeByIdUC: GetOfficeByIdUC) {
        viewModelScope.launch {
            val resultado = getOfficeByIdUC.invoke(idOffice).getOrNull()
            _selectedOffice.value = resultado
        }
    }
    fun cargarConsultoriosPorEspecialidad(idCenter: String, specialty: String, getOfficesUC: GetOfficesByCenterAndSpecialtyUC) {
        viewModelScope.launch {
            val resultado = getOfficesUC.invoke(idCenter, specialty).getOrNull()
            _officesList.value = resultado ?: emptyList()
        }
    }

    fun cargarConsultoriosPorDoctor(idDoctor: String, getOfficesByDoctorUC: GetOfficesByDoctorUC) {
        viewModelScope.launch {
            val resultado = getOfficesByDoctorUC.invoke(idDoctor).getOrNull()
            _officesList.value = resultado ?: emptyList()
        }
    }

    fun registrarConsultorio(office: ConsultorioDtoRemote, saveOfficeUC: SaveOfficeUC) {
        viewModelScope.launch {
            val resultado = saveOfficeUC.invoke(office).isSuccess
            _operationResult.value = resultado
        }
    }

    fun actualizarHorarios(
        idOffice: String,
        availableDays: List<String>,
        openingTime: String,
        closingTime: String,
        updateHorariesUC: UpdateOfficeHorariesUC
    ) {
        viewModelScope.launch {
            val resultado = updateHorariesUC.invoke(idOffice, availableDays, openingTime, closingTime).isSuccess
            _operationResult.value = resultado
        }
    }
}