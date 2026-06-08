package com.example.mapsalud.medicPages

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

class AgregarConsultorio : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: MedicAgregarConsultorioBinding
    private lateinit var mapa: GoogleMap
    private val diasSeleccionados = mutableSetOf<String>()

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MedicAgregarConsultorioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment = supportFragmentManager.findFragmentById(binding.mapConsultorio.id) as SupportMapFragment
        mapFragment.getMapAsync(this)

        configurarPanelDeslizable()
        initListeners()
        configurarBotonesDias()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mapa = googleMap

        mapa.uiSettings.isZoomGesturesEnabled = true
        mapa.uiSettings.isZoomControlsEnabled = true

        val centroSalud = LatLng(-0.180653, -78.467834)
        mapa.addMarker(MarkerOptions().position(centroSalud).title("Centro de Salud Iñaquito"))
        mapa.moveCamera(CameraUpdateFactory.newLatLngZoom(centroSalud, 15f))

        val bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheetForm)

        mapa.setOnMarkerClickListener { marker ->
            binding.cardUbicacion.visibility = View.VISIBLE
            binding.txtNombreCentro.text = marker.title
            binding.txtDireccionCentro.text = "Av. Naciones Unidas y Núñez de Vela"
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED

            true
        }

        mapa.setOnMapClickListener {
            binding.cardUbicacion.visibility = View.GONE

            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }
    }

    private fun configurarPanelDeslizable() {
        bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheetForm)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED

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
                    BottomSheetBehavior.STATE_COLLAPSED -> binding.btnToggleSheet.rotation = 180f // Flecha hacia arriba
                    BottomSheetBehavior.STATE_EXPANDED -> binding.btnToggleSheet.rotation = 0f    // Flecha hacia abajo
                }
            }
            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        })
    }

    private fun initListeners() {
        binding.btnRegresar.setOnClickListener {
            finish()
        }

        binding.btnGuardarConsultorio.setOnClickListener {

            if (binding.cardUbicacion.visibility == View.GONE) {
                Toast.makeText(this, "Por favor, selecciona un consultorio en el mapa primero", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val especialidad = binding.inputEspecialidad.text.toString().trim()
            if (especialidad.isEmpty() || diasSeleccionados.isEmpty()) {
                Toast.makeText(this, "Por favor llena la especialidad y elige al menos un día", Toast.LENGTH_SHORT).show()
                return@setOnClickListener 
            }

            Toast.makeText(this, "Consultorio agregado exitosamente", Toast.LENGTH_SHORT).show()
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
            textView.setBackgroundColor(Color.parseColor("#E0E0E0"))
            textView.setTextColor(Color.parseColor("#757575"))

            textView.setOnClickListener {
                if (diasSeleccionados.contains(dia)) {
                    diasSeleccionados.remove(dia)
                    textView.setBackgroundColor(Color.parseColor("#E0E0E0"))
                    textView.setTextColor(Color.parseColor("#757575"))
                } else {
                    diasSeleccionados.add(dia)
                    textView.setBackgroundColor(Color.parseColor("#00695C"))
                    textView.setTextColor(Color.WHITE)
                }
            }
        }
    }
}