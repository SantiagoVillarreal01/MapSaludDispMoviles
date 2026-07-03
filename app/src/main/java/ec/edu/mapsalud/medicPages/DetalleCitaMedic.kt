package ec.edu.mapsalud.medicPages

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast
import androidx.activity.viewModels
import ec.edu.mapsalud.databinding.MedicDetalleCitaBinding
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ec.edu.mapsalud.remote.impl.CitaRepositoryImpl
import ec.edu.mapsalud.remote.impl.UsuariosRepositoryImpl
import ec.edu.mapsalud.remote.inter.CitaRepository
import ec.edu.mapsalud.remote.inter.UsuariosRepository
import ec.edu.mapsalud.usercases.citasUC.GetAppointmentByIdUC
import ec.edu.mapsalud.usercases.citasUC.UpdateAppointmentStatusUC
import ec.edu.mapsalud.usercases.usuariosUC.GetPacienteByIdUC
import ec.edu.mapsalud.utils.ThemeUtils
import ec.edu.mapsalud.viewmodel.CitaViewModel
import ec.edu.mapsalud.viewmodel.UsuarioViewModel

class DetalleCitaMedic : AppCompatActivity() {

    private lateinit var binding: MedicDetalleCitaBinding
    private var idCitaActual: String? = null

    private val citaVM by viewModels<CitaViewModel>()
    private val usuarioVM by viewModels<UsuarioViewModel>()
    private val citaRepository = CitaRepositoryImpl()
    private val usuarioRepository = UsuariosRepositoryImpl()

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeUtils.applyTheme(this)
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
        initObservers()
        cargarDatosDeLaCita()
    }

    private fun cargarDatosDeLaCita() {
        binding.btnGuardarEstado.isEnabled = false
        citaVM.obtenerCitaPorId(idCitaActual!!, GetAppointmentByIdUC(citaRepository))
    }

    private fun initObservers() {
        citaVM.selectedAppointment.observe(this) { cita ->
            if (cita != null) {
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

                usuarioVM.cargarPaciente(cita.idUser, GetPacienteByIdUC(usuarioRepository))
            } else {
                Toast.makeText(this, "Error: Cita no encontrada", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        usuarioVM.paciente.observe(this) { paciente ->
            if (paciente != null) {
                binding.txtNombreDetalle.text = "${paciente.info.nombres} ${paciente.info.apellidos}"
                binding.txtCedulaDetalle.text = "C.I: ${paciente.info.cedula}"
                binding.txtTelefonoDetalle.text = "Teléfono: ${paciente.info.telefono}"

                val generoPaciente = paciente.info.genero.ifEmpty { "No especificado" }
                binding.txtGeneroDetalle.text = "Género: $generoPaciente"
                binding.txtSangreDetalle.text = "Tipo de Sangre: ${paciente.tipoSangre.nombreMostrar}"
                binding.btnGuardarEstado.isEnabled = true
            } else {
                Toast.makeText(this, "Error: Paciente no encontrado", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        citaVM.operationSuccess.observe(this) { exito ->
            if (exito) {
                val estadoSeleccionado = obtenerEstadoDesdeUI() ?: "Actualizada"
                Toast.makeText(this, "Cita marcada como $estadoSeleccionado", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Error al actualizar el estado", Toast.LENGTH_SHORT).show()
                binding.btnGuardarEstado.isEnabled = true
            }
        }
    }

    private fun configurarBotones() {
        binding.btnRegresar.setOnClickListener {
            finish()
        }

        binding.btnGuardarEstado.setOnClickListener {
            val estadoSeleccionado = obtenerEstadoDesdeUI()

            if (estadoSeleccionado != null) {
                actualizarEstadoCita(estadoSeleccionado)
            } else {
                Toast.makeText(this, "Por favor selecciona un estado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun actualizarEstadoCita(nuevoEstado: String) {
        binding.btnGuardarEstado.isEnabled = false

        citaVM.cambiarEstadoCita(
            appointmentId = idCitaActual!!,
            status = nuevoEstado,
            updateStatusUC = UpdateAppointmentStatusUC(citaRepository)
        )
    }


    private fun obtenerEstadoDesdeUI(): String? {
        return when (binding.radioGroupEstado.checkedRadioButtonId) {
            binding.radioAtendida.id -> "Completada"
            binding.radioInasistencia.id -> "Inasistencia"
            binding.radioCancelada.id -> "Cancelada"
            else -> null
        }
    }

}