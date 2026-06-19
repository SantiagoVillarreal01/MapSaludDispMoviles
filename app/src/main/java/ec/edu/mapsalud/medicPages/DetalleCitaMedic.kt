package ec.edu.mapsalud.medicPages

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast
import ec.edu.mapsalud.databinding.MedicDetalleCitaBinding
import androidx.lifecycle.lifecycleScope
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import ec.edu.mapsalud.dto.AppointmentDtoRemote
import ec.edu.mapsalud.dto.Paciente

class DetalleCitaMedic : AppCompatActivity() {

    private lateinit var binding: MedicDetalleCitaBinding
    private val db = FirebaseFirestore.getInstance()
    private var idCitaActual: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MedicDetalleCitaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        idCitaActual = intent.getStringExtra("ID_CITA")

        if (idCitaActual.isNullOrEmpty()) {
            Toast.makeText(this, "Error al cargar la cita.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        configurarBotones()
        cargarDatosDeLaCita()
    }

    private fun cargarDatosDeLaCita() {
        binding.btnGuardarEstado.isEnabled = false

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val citaDoc = db.collection("citas").document(idCitaActual!!).get().await()
                val cita = citaDoc.toObject(AppointmentDtoRemote::class.java) ?: throw Exception("Cita nula")

                val pacienteDoc = db.collection("usuarios").document(cita.idUser).get().await()
                val paciente = pacienteDoc.toObject(Paciente::class.java) ?: throw Exception("Paciente nulo")

                withContext(Dispatchers.Main) {
                    binding.txtNombreDetalle.text = "${paciente.info.nombres} ${paciente.info.apellidos}"
                    binding.txtCedulaDetalle.text = "C.I: ${paciente.info.cedula}"
                    binding.txtTelefonoDetalle.text = "Teléfono: ${paciente.info.telefono}"

                    val generoPaciente = paciente.info.genero.ifEmpty { "No especificado" }
                    binding.txtGeneroDetalle.text = "Género: $generoPaciente"

                    binding.txtSangreDetalle.text = "Tipo de Sangre: ${paciente.tipoSangre.nombreMostrar}"

                    binding.txtFechaHoraDetalle.text = "📅 ${cita.date} • ${cita.time}"
                    binding.txtEstadoDetalle.text = cita.status
                    binding.txtReasonDetalle.text = cita.reason

                    if (cita.description.isNotEmpty()) {
                        binding.txtDescriptionDetalle.text = cita.description
                    } else {
                        binding.txtDescriptionDetalle.text = "El paciente no añadió una descripción adicional."
                    }

                    when (cita.status) {
                        "Completada" -> binding.radioAtendida.isChecked = true
                        "Inasistencia" -> binding.radioInasistencia.isChecked = true
                        "Cancelada" -> binding.radioCancelada.isChecked = true
                    }

                    binding.btnGuardarEstado.isEnabled = true
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@DetalleCitaMedic, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    private fun configurarBotones() {
        binding.btnRegresar.setOnClickListener {
            finish()
        }

        binding.btnGuardarEstado.setOnClickListener {
            val estadoSeleccionado = when (binding.radioGroupEstado.checkedRadioButtonId) {
                binding.radioAtendida.id -> "Completada"
                binding.radioInasistencia.id -> "Inasistencia"
                binding.radioCancelada.id -> "Cancelada"
                else -> null
            }

            if (estadoSeleccionado != null) {
                actualizarEstadoCita(estadoSeleccionado)
            } else {
                Toast.makeText(this, "Por favor selecciona un estado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun actualizarEstadoCita(nuevoEstado: String) {
        binding.btnGuardarEstado.isEnabled = false

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                db.collection("citas").document(idCitaActual!!)
                    .update("status", nuevoEstado).await()

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@DetalleCitaMedic, "Cita marcada como $nuevoEstado", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@DetalleCitaMedic, "Error al actualizar", Toast.LENGTH_SHORT).show()
                    binding.btnGuardarEstado.isEnabled = true
                }
            }
        }
    }
}