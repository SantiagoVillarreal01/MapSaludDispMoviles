package ec.edu.mapsalud.medicPages

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import ec.edu.mapsalud.databinding.MedicAgregarConsultorioBinding
import kotlin.collections.iterator
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Marker
import com.google.firebase.auth.FirebaseAuth
import ec.edu.mapsalud.dto.CentroMedicoDtoRemote
import ec.edu.mapsalud.dto.ConsultorioDtoRemote
import ec.edu.mapsalud.enum.Specialty
import android.Manifest
import android.content.pm.PackageManager
import android.text.Editable
import android.widget.ListPopupWindow
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import ec.edu.mapsalud.enum.CenterType
import ec.edu.mapsalud.utils.ThemeUtils
import ec.edu.mapsalud.remote.impl.CentroMedicoRepositoryImpl
import ec.edu.mapsalud.remote.impl.ConsultorioRepositoryImpl
import ec.edu.mapsalud.usercases.centrosUC.AddSpecialtyToCenterUC
import ec.edu.mapsalud.usercases.centrosUC.GetAllCentersUC
import ec.edu.mapsalud.usercases.consultoriosUC.SaveOfficeUC
import ec.edu.mapsalud.viewmodel.CentroMedicoViewModel
import ec.edu.mapsalud.viewmodel.ConsultorioViewModel
import android.content.Context
import android.text.TextWatcher
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import java.text.Normalizer

class AgregarConsultorio : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: MedicAgregarConsultorioBinding
    private lateinit var mapa: GoogleMap
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>

    private lateinit var searchPopupWindow: ListPopupWindow

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val centroMedicoVM by viewModels<CentroMedicoViewModel>()
    private val consultorioVM by viewModels<ConsultorioViewModel>()

    private val centroMedicoRepository = CentroMedicoRepositoryImpl()
    private val consultorioRepository = ConsultorioRepositoryImpl()
    private val auth = FirebaseAuth.getInstance()

    private val diasSeleccionados = mutableSetOf<String>()
    private var centroSeleccionado: CentroMedicoDtoRemote? = null
    private var especialidadSeleccionada: Specialty? = null
    private val marcadoresCentrosMedicos = HashMap<Marker, CentroMedicoDtoRemote>()
    private var listaCentrosCompleta: List<CentroMedicoDtoRemote> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeUtils.applyTheme(this)
        super.onCreate(savedInstanceState)
        binding = MedicAgregarConsultorioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager.findFragmentById(binding.mapConsultorio.id) as SupportMapFragment
        mapFragment.getMapAsync(this)

        searchPopupWindow = ListPopupWindow(this)

        configurarPanelDeslizable()
        configurarDropdownEspecialidades()
        configurarBuscador()
        initListeners()
        initObservers()
        configurarBotonesDias()
    }

    private fun initObservers() {
        centroMedicoVM.centersList.observe(this) { listaCentros ->
            listaCentrosCompleta = listaCentros
            mapa.clear()
            marcadoresCentrosMedicos.clear()

            for (centro in listaCentros) {
                val coordenadas = LatLng(centro.latitude, centro.longitude)
                val markerOptions = MarkerOptions()
                    .position(coordenadas)
                    .title(centro.name)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))

                val m = mapa.addMarker(markerOptions)
                if (m != null) {
                    marcadoresCentrosMedicos[m] = centro
                }
            }
        }

        consultorioVM.operationResult.observe(this) { exito ->
            if (exito) {
                Toast.makeText(this, "¡Consultorio vinculado correctamente!", Toast.LENGTH_LONG).show()
                finish()
            } else {
                Toast.makeText(this, "Error al guardar el consultorio", Toast.LENGTH_SHORT).show()
                binding.btnGuardarConsultorio.isEnabled = true
            }
        }
    }

    private fun configurarBuscador() {
        binding.editBuscarCentro.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filtrarCentrosMedicos(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filtrarCentrosMedicos(query: String) {
        if (query.isBlank()) {
            searchPopupWindow.dismiss()
            return
        }

        val queryLimpio = quitarTildes(query).lowercase()
        val filtrados = listaCentrosCompleta.filter { centro ->
            quitarTildes(centro.name).lowercase().contains(queryLimpio) ||
                    quitarTildes(centro.address).lowercase().contains(queryLimpio)
        }

        if (filtrados.isEmpty()) {
            searchPopupWindow.dismiss()
            return
        }

        val infoFiltrada = filtrados.map { "${it.name}\n${it.address}" }
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, infoFiltrada)

        searchPopupWindow.setAdapter(adapter)
        searchPopupWindow.anchorView = binding.searchCard
        searchPopupWindow.isModal = false

        searchPopupWindow.setOnItemClickListener { _, _, position, _ ->
            val centroElegido = filtrados[position]
            binding.editBuscarCentro.setText(centroElegido.name)
            searchPopupWindow.dismiss()
            ocultarTeclado()

            val marcadorAsociado = marcadoresCentrosMedicos.entries.find { it.value.id == centroElegido.id }?.key
            val ubicacion = marcadorAsociado?.position ?: LatLng(centroElegido.latitude, centroElegido.longitude)

            mostrarDetalleCentro(centroElegido, ubicacion)
        }

        if (!searchPopupWindow.isShowing) {
            searchPopupWindow.show()
        }
    }

    private fun mostrarDetalleCentro(centro: CentroMedicoDtoRemote, posicion: LatLng) {
        centroSeleccionado = centro
        binding.cardUbicacion.visibility = View.VISIBLE

        val tipoCentro = try {
            CenterType.valueOf(centro.type).nombreMostrar
        } catch (e: Exception) {
            "Centro Médico"
        }

        binding.txtNombreCentro.text = centro.name
        binding.txtDireccionCentro.text = "${centro.address} • $tipoCentro"
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        mapa.animateCamera(CameraUpdateFactory.newLatLngZoom(posicion, 16f))
    }

    private fun quitarTildes(texto: String): String {
        val normalizado = Normalizer.normalize(texto, Normalizer.Form.NFD)
        return normalizado.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
    }

    private fun ocultarTeclado() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        currentFocus?.let { view ->
            imm.hideSoftInputFromWindow(view.windowToken, 0)
            view.clearFocus()
        }
    }

    private fun configurarDropdownEspecialidades() {
        val listaEspecialidades = Specialty.entries.sortedBy { it.nombreMostrar }
        val nombresMostrar = listaEspecialidades.map { it.nombreMostrar }.toTypedArray()
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, nombresMostrar)
        binding.autoCompleteEspecialidad.setAdapter(adapter)

        binding.autoCompleteEspecialidad.setOnItemClickListener { _, _, _, _ ->
            val nombreSeleccionado = binding.autoCompleteEspecialidad.text.toString()
            especialidadSeleccionada = Specialty.entries.find { it.nombreMostrar == nombreSeleccionado }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mapa = googleMap
        mapa.uiSettings.isZoomGesturesEnabled = true
        mapa.uiSettings.isZoomControlsEnabled = true

        configurarUbicacionEnTiempoReal()
        obtenerCentrosMedicosDesdeRepositorio()

        if (ThemeUtils.isDark(this)) {
            try {
                mapa.setMapStyle(com.google.android.gms.maps.model.MapStyleOptions.loadRawResourceStyle(this, ec.edu.mapsalud.R.raw.map_style_dark))
            } catch (e: Exception) { e.printStackTrace() }
        }

        mapa.setOnMarkerClickListener { marker ->
            val centro = marcadoresCentrosMedicos[marker]
            if (centro != null) {
                mostrarDetalleCentro(centro, marker.position)
            }
            true
        }

        mapa.setOnMapClickListener {
            centroSeleccionado = null
            binding.cardUbicacion.visibility = View.GONE
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            binding.editBuscarCentro.text?.clear()
            searchPopupWindow.dismiss()
            ocultarTeclado()
        }
    }

    private fun obtenerCentrosMedicosDesdeRepositorio() {
        centroMedicoVM.cargarTodosLosCentros(GetAllCentersUC(centroMedicoRepository))
    }

    private fun configurarPanelDeslizable() {
        bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheetForm)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

        binding.btnToggleSheet.setOnClickListener {
            if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            } else {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }

        bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_COLLAPSED -> binding.btnToggleSheet.rotation = 180f
                    BottomSheetBehavior.STATE_EXPANDED -> binding.btnToggleSheet.rotation = 0f
                }
            }
            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        })
    }

    private fun configurarUbicacionEnTiempoReal() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                1001
            )
            return
        }
        mapa.isMyLocationEnabled = true

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val userLocation = LatLng(location.latitude, location.longitude)
                mapa.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 14f))
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            configurarUbicacionEnTiempoReal()
        }
    }

    private fun initListeners() {
        binding.btnRegresar.setOnClickListener {
            finish()
        }

        binding.inputApertura.setOnClickListener {
            mostrarSelectorHora("Hora de apertura") { hora, minuto ->
                binding.inputApertura.setText(formatearHoraAMPM(hora, minuto))
            }
        }

        binding.inputCierre.setOnClickListener {
            mostrarSelectorHora("Hora de cierre") { hora, minuto ->
                binding.inputCierre.setText(formatearHoraAMPM(hora, minuto))
            }
        }

        binding.btnGuardarConsultorio.setOnClickListener {
            ejecutarGuardadoConsultorio()
        }
    }

    private fun ejecutarGuardadoConsultorio() {
        val centro = centroSeleccionado
        val especialidad = especialidadSeleccionada
        val horaApertura = binding.inputApertura.text.toString().trim()
        val horaCierre = binding.inputCierre.text.toString().trim()
        val idMedicoLogueado = auth.currentUser?.uid ?: ""

        if (centro == null) {
            Toast.makeText(this, "Por favor, selecciona un centro médico en el mapa", Toast.LENGTH_SHORT).show()
            return
        }
        if (especialidad == null) {
            Toast.makeText(this, "Debe seleccionar una especialidad médica", Toast.LENGTH_SHORT).show()
            return
        }
        if (diasSeleccionados.isEmpty()) {
            Toast.makeText(this, "Seleccione al menos un día de atención", Toast.LENGTH_SHORT).show()
            return
        }
        if (idMedicoLogueado.isEmpty()) {
            Toast.makeText(this, "Error de autenticación médico no válido", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnGuardarConsultorio.isEnabled = false

        val nuevoIdOffice = consultorioRepository.generateNewOfficeId()
        val nuevoConsultorio = ConsultorioDtoRemote(
            id = nuevoIdOffice,
            idCenter = centro.id,
            idDoctor = idMedicoLogueado,
            specialty = especialidad.name,
            availableDays = diasSeleccionados.toList(),
            openingTime = horaApertura,
            closingTime = horaCierre
        )

        consultorioVM.registrarConsultorio(nuevoConsultorio, SaveOfficeUC(consultorioRepository))

        centroMedicoVM.agregarEspecialidad(
            idCenter = centro.id,
            specialty = especialidad.name,
            addSpecialtyUC = AddSpecialtyToCenterUC(centroMedicoRepository)
        )
    }

    private fun mostrarSelectorHora(titulo: String, onTimeSelected: (Int, Int) -> Unit) {
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_12H)
            .setHour(8)
            .setMinute(0)
            .setTitleText(titulo)
            .build()

        picker.addOnPositiveButtonClickListener {
            onTimeSelected(picker.hour, picker.minute)
        }
        picker.show(supportFragmentManager, "time_picker")
    }

    private fun formatearHoraAMPM(hora: Int, minuto: Int): String {
        val amPm = if (hora >= 12) "PM" else "AM"
        val horaFormateada = if (hora % 12 == 0) 12 else hora % 12
        return String.format("%02d:%02d %s", horaFormateada, minuto, amPm)
    }

    private fun configurarBotonesDias() {
        val botonesDias = mapOf(
            binding.btnDiaL to "Lunes",
            binding.btnDiaM to "Martes",
            binding.btnDiaMi to "Miércoles",
            binding.btnDiaJ to "Jueves",
            binding.btnDiaV to "Viernes",
            binding.btnDiaS to "Sábado",
            binding.btnDiaD to "Domingo"
        )

        for ((textView, dia) in botonesDias) {
            val estaSeleccionado = diasSeleccionados.contains(dia)
            textView.setBackgroundResource(if (estaSeleccionado) ec.edu.mapsalud.R.drawable.chip_day_selected else ec.edu.mapsalud.R.drawable.chip_day_unselected)
            textView.setTextColor(if (estaSeleccionado) Color.WHITE else Color.parseColor("#757575"))

            textView.setOnClickListener {
                if (diasSeleccionados.contains(dia)) {
                    diasSeleccionados.remove(dia)
                } else {
                    diasSeleccionados.add(dia)
                }
                configurarBotonesDias()
            }
        }
    }
}