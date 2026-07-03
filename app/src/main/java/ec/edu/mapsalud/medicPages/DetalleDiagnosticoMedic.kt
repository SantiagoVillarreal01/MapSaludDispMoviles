package ec.edu.mapsalud.medicPages

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import ec.edu.mapsalud.databinding.MedicDetalleDiagnosticoBinding
import ec.edu.mapsalud.dto.DiagnosisEmbedded
import ec.edu.mapsalud.remote.impl.CitaRepositoryImpl
import ec.edu.mapsalud.remote.impl.UsuariosRepositoryImpl
import ec.edu.mapsalud.remote.inter.CitaRepository
import ec.edu.mapsalud.usercases.citasUC.UpdateAppointmentDiagnosisUC
import ec.edu.mapsalud.utils.ThemeUtils
import ec.edu.mapsalud.viewmodel.CitaViewModel
import ec.edu.mapsalud.viewmodel.UsuarioViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DetalleDiagnosticoMedic : AppCompatActivity() {

    private lateinit var binding: MedicDetalleDiagnosticoBinding
    private var appointmentId: String = ""
    private val citaRepository = CitaRepositoryImpl()
    private val citaVM: CitaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeUtils.applyTheme(this)
        super.onCreate(savedInstanceState)
        binding = MedicDetalleDiagnosticoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        appointmentId = intent.getStringExtra("APPOINTMENT_ID") ?: ""
        val nombrePaciente = intent.getStringExtra("PACIENTE_NOMBRE") ?: "Desconocido"
        val motivoCita = intent.getStringExtra("MOTIVO_CITA") ?: "Sin motivo"

        binding.txtPacienteDetalle.text = "Paciente: $nombrePaciente"
        binding.txtMotivoDetalle.text = "Motivo: $motivoCita"

        initObservers()

        binding.btnRegresarDiag.setOnClickListener {
            finish()
        }

        binding.btnEnviarDiagnostico.setOnClickListener {
            guardarDiagnosticoEnRepositorio()
        }
    }

    private fun initObservers() {
        citaVM.operationSuccess.observe(this) { exito ->
            if (exito) {
                Toast.makeText(
                    this,
                    "Diagnóstico guardado exitosamente",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            } else {
                binding.btnEnviarDiagnostico.isEnabled = true
                binding.btnEnviarDiagnostico.text = "Guardar y Completar Cita"
                Toast.makeText(
                    this,
                    "Error al guardar el diagnóstico",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun guardarDiagnosticoEnRepositorio() {
        val diagnosticoTxt = binding.inputDiagnostico.text.toString().trim()
        val tratamientoTxt = binding.inputTratamiento.text.toString().trim()
        val sugerenciasTxt = binding.inputSugerencias.text.toString().trim()

        if (diagnosticoTxt.isEmpty()) {
            Toast.makeText(this, "El diagnóstico clínico es obligatorio", Toast.LENGTH_SHORT).show()
            return
        }

        if (appointmentId.isEmpty()) {
            Toast.makeText(this, "Error: No se encontró el ID de la cita", Toast.LENGTH_SHORT)
                .show()
            return
        }

        binding.btnEnviarDiagnostico.isEnabled = false
        binding.btnEnviarDiagnostico.text = "Guardando..."

        val formatoFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val fechaActual = formatoFecha.format(Date())

        val nuevoDiagnostico = DiagnosisEmbedded(
            clinicalDiagnosis = diagnosticoTxt,
            treatment = tratamientoTxt,
            suggestions = sugerenciasTxt,
            dateGiven = fechaActual
        )

        citaVM.finalizarCitaConDiagnostico(
            appointmentId = appointmentId,
            diagnosis = nuevoDiagnostico,
            updateDiagnosisUC = UpdateAppointmentDiagnosisUC(citaRepository)
        )
    }
}