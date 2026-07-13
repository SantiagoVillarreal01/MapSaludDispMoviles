package ec.edu.mapsalud.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ec.edu.mapsalud.dto.CentroMedicoDtoRemote
import ec.edu.mapsalud.usercases.centrosUC.AddSpecialtyToCenterUC
import ec.edu.mapsalud.usercases.centrosUC.GetAllCentersUC
import ec.edu.mapsalud.usercases.centrosUC.GetCenterByIdUC
import ec.edu.mapsalud.usercases.centrosUC.GetCentersFilteredUC
import kotlinx.coroutines.launch

class CentroMedicoViewModel : ViewModel() {

    private var _centersList = MutableLiveData<List<CentroMedicoDtoRemote>>()
    val centersList: LiveData<List<CentroMedicoDtoRemote>> get() = _centersList

    private var _filteredCenters = MutableLiveData<List<Pair<CentroMedicoDtoRemote, Float>>>()
    val filteredCenters: LiveData<List<Pair<CentroMedicoDtoRemote, Float>>> get() = _filteredCenters

    private var _selectedCenter = MutableLiveData<CentroMedicoDtoRemote?>()
    val selectedCenter: LiveData<CentroMedicoDtoRemote?> get() = _selectedCenter

    private var _searchSuggestions = MutableLiveData<List<CentroMedicoDtoRemote>>()
    val searchSuggestions: LiveData<List<CentroMedicoDtoRemote>> get() = _searchSuggestions

    fun cargarTodosLosCentros(getAllCentersUC: GetAllCentersUC) {
        viewModelScope.launch {
            val resultado = getAllCentersUC.invoke().getOrNull()
            _centersList.value = resultado ?: emptyList()
        }
    }

    fun obtenerCentroPorId(id: String, getCenterByIdUC: GetCenterByIdUC) {
        viewModelScope.launch {
            val resultado = getCenterByIdUC.invoke(id).getOrNull()
            _selectedCenter.value = resultado
        }
    }

    fun filtrarCentrosPorUbicacion(
        userLat: Double,
        userLon: Double,
        type: String?,
        specialty: String?,
        radiusInMeters: Double,
        getCentersFilteredUC: GetCentersFilteredUC
    ) {
        viewModelScope.launch {
            val resultado = getCentersFilteredUC.invoke(userLat, userLon, type, specialty, radiusInMeters).getOrNull()
            _filteredCenters.value = resultado ?: emptyList()
        }
    }

    fun agregarEspecialidad(idCenter: String, specialty: String, addSpecialtyUC: AddSpecialtyToCenterUC) {
        viewModelScope.launch {
            addSpecialtyUC.invoke(idCenter, specialty)
        }
    }

    fun filtrarSugerenciasPorNombre(query: String) {
        val listaTotal = _centersList.value ?: emptyList()

        if (query.isBlank()) {
            _searchSuggestions.value = emptyList()
            return
        }

        val filtrados = listaTotal.filter { centro ->
            centro.name.contains(query, ignoreCase = true)
        }
        _searchSuggestions.value = filtrados
    }

    fun seleccionarCentro(centro: CentroMedicoDtoRemote) {
        _selectedCenter.value = centro
    }

    fun deseleccionarCentro() {
        _selectedCenter.value = null
    }
}