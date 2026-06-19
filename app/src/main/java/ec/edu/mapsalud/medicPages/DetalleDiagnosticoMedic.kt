package ec.edu.mapsalud.medicPages

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore
import ec.edu.mapsalud.databinding.MedicDetalleDiagnosticoBinding
import ec.edu.mapsalud.dto.DiagnosisEmbedded
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DetalleDiagnosticoMedic : AppCompatActivity() {

    private lateinit var binding: MedicDetalleDiagnosticoBinding
    private val db = FirebaseFirestore.getInstance()
    private var appointmentId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MedicDetalleDiagnosticoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        appointmentId = intent.getStringExtra("APPOINTMENT_ID") ?: ""
        val nombrePaciente = intent.getStringExtra("PACIENTE_NOMBRE") ?: "Desconocido"
        val motivoCita = intent.getStringExtra("MOTIVO_CITA") ?: "Sin motivo"

        binding.txtPacienteDetalle.text = "Paciente: $nombrePaciente"
        binding.txtMotivoDetalle.text = "Motivo: $motivoCita"

        binding.btnRegresarDiag.setOnClickListener {
            finish()
        }

        binding.btnEnviarDiagnostico.setOnClickListener {
            guardarDiagnosticoEnFirebase()
        }
    }

    private fun guardarDiagnosticoEnFirebase() {
        val diagnosticoTxt = binding.inputDiagnostico.text.toString().trim()
        val tratamientoTxt = binding.inputTratamiento.text.toString().trim()
        val sugerenciasTxt = binding.inputSugerencias.text.toString().trim()

        if (diagnosticoTxt.isEmpty()) {
            Toast.makeText(this, "El diagnóstico clínico es obligatorio", Toast.LENGTH_SHORT).show()
            return
        }

        if (appointmentId.isEmpty()) {
            Toast.makeText(this, "Error: No se encontró el ID de la cita", Toast.LENGTH_SHORT).show()
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

        db.collection("citas").document(appointmentId)
            .update(
                mapOf(
                    "diagnosis" to nuevoDiagnostico,
                    "status" to "Completada"
                )
            )
            .addOnSuccessListener {
                Toast.makeText(this, "Diagnóstico guardado exitosamente", Toast.LENGTH_LONG).show()
                finish()
            }
            .addOnFailureListener { e ->
                binding.btnEnviarDiagnostico.isEnabled = true
                binding.btnEnviarDiagnostico.text = "Guardar y Completar Cita"
                Toast.makeText(this, "Error al guardar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}