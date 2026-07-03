package ec.edu.mapsalud.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ec.edu.mapsalud.dto.Medico
import ec.edu.mapsalud.dto.Paciente
import ec.edu.mapsalud.usercases.usuariosUC.GetDoctorByIdUC
import ec.edu.mapsalud.usercases.usuariosUC.GetPacienteByCedulaUC
import ec.edu.mapsalud.usercases.usuariosUC.GetPacienteByIdUC
import kotlinx.coroutines.launch

class UsuarioViewModel : ViewModel() {

    private var _paciente = MutableLiveData<Paciente?>()
    val paciente: LiveData<Paciente?> get() = _paciente

    private var _medico = MutableLiveData<Medico?>()
    val medico: LiveData<Medico?> get() = _medico

    fun cargarPaciente(idUser: String, getPacienteByIdUC: GetPacienteByIdUC) {
        viewModelScope.launch {
            val resultado = getPacienteByIdUC.invoke(idUser).getOrNull()
            _paciente.value = resultado
        }
    }

    fun cargarMedico(idDoctor: String, getDoctorByIdUC: GetDoctorByIdUC) {
        viewModelScope.launch {
            val resultado = getDoctorByIdUC.invoke(idDoctor).getOrNull()
            _medico.value = resultado
        }
    }

    fun buscarPacientePorCedula(cedula: String, getPacienteByCedulaUC: GetPacienteByCedulaUC) {
        viewModelScope.launch {
            val resultado = getPacienteByCedulaUC.invoke(cedula).getOrNull()
            _paciente.value = resultado
        }
    }
}