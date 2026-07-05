package ec.edu.mapsalud.medicPages

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import ec.edu.mapsalud.databinding.MedicDetalleDiagnosticoBinding
import ec.edu.mapsalud.dto.Diagnostico
import ec.edu.mapsalud.remote.impl.CitaRepositoryImpl
import ec.edu.mapsalud.usercases.citasUC.UpdateAppointmentDiagnosisUC
import ec.edu.mapsalud.utils.ThemeUtils
import ec.edu.mapsalud.viewmodel.CitaViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DetalleDiagnosticoMedic : AppCompatActivity() {

    private lateinit var binding: MedicDetalleDiagnosticoBinding
    private var appointmentId: String = ""
    private val citaRepository = CitaRepositoryImpl()
    private val citaVM: CitaViewModel by viewModels()

    private var idCampoObjetivo: Int = 0

    private val speechLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { resultado ->
        if (resultado.resultCode == Activity.RESULT_OK && resultado.data != null) {
            val palabrasReconocidas = resultado.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!palabrasReconocidas.isNullOrEmpty()) {
                val textoDictado = palabrasReconocidas[0]

                when (idCampoObjetivo) {
                    binding.inputDiagnostico.id -> {
                        binding.inputDiagnostico.setText(textoDictado)
                        binding.inputDiagnostico.setSelection(textoDictado.length)
                    }
                    binding.inputTratamiento.id -> {
                        binding.inputTratamiento.setText(textoDictado)
                        binding.inputTratamiento.setSelection(textoDictado.length)
                    }
                    binding.inputSugerencias.id -> {
                        binding.inputSugerencias.setText(textoDictado)
                        binding.inputSugerencias.setSelection(textoDictado.length)
                    }
                }
            }
        }
    }

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

        configurarBotonesDeDictado()
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

    private fun configurarBotonesDeDictado() {
        binding.tilDiagnostico.setEndIconOnClickListener {
            iniciarDictadoPorVoz(binding.inputDiagnostico.id)
        }

        binding.tilTratamiento.setEndIconOnClickListener {
            iniciarDictadoPorVoz(binding.inputTratamiento.id)
        }

        binding.tilSugerencias.setEndIconOnClickListener {
            iniciarDictadoPorVoz(binding.inputSugerencias.id)
        }
    }

    private fun iniciarDictadoPorVoz(targetEditTextId: Int) {
        idCampoObjetivo = targetEditTextId

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Dictando datos médicos...")
        }

        try {
            speechLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "El reconocimiento de voz no está disponible en este dispositivo", Toast.LENGTH_SHORT).show()
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

        val nuevoDiagnostico = Diagnostico(
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