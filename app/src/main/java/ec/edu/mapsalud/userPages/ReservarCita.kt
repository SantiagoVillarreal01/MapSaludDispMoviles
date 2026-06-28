package ec.edu.mapsalud.userPages

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import ec.edu.mapsalud.databinding.UserReservarCitaBinding
import android.graphics.Color
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import ec.edu.mapsalud.dto.AppointmentDtoRemote
import ec.edu.mapsalud.dto.OfficeDtoRemote
import ec.edu.mapsalud.dto.Paciente
import ec.edu.mapsalud.remote.impl.AppointmentRemoteImpl
import ec.edu.mapsalud.remote.inter.AppointmentRemote
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import ec.edu.mapsalud.utils.ThemeUtils

class ReservarCita : AppCompatActivity() {

    private lateinit var binding: UserReservarCitaBinding
    private val repository: AppointmentRemote = AppointmentRemoteImpl()
    private val auth = FirebaseAuth.getInstance()

    private var currentOffice: OfficeDtoRemote? = null
    private var selectedDate: String = ""
    private var selectedTime: String = ""
    private var selectedTimeButton: Button? = null

    private val diasSemanaMap = mapOf(
        Calendar.SUNDAY to "Domingo", Calendar.MONDAY to "Lunes",
        Calendar.TUESDAY to "Martes", Calendar.WEDNESDAY to "Miércoles",
        Calendar.THURSDAY to "Jueves", Calendar.FRIDAY to "Viernes",
        Calendar.SATURDAY to "Sábado"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeUtils.applyTheme(this)
        super.onCreate(savedInstanceState)
        binding = UserReservarCitaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val doctorName = intent.getStringExtra("NOMBRE_DOCTOR") ?: ""
        val idOffice = intent.getStringExtra("ID_OFFICE") ?: ""

        val idDoctor = intent.getStringExtra("ID_DOCTOR") ?: ""

        binding.txtNombreDoctor.text = doctorName
        binding.btnRegresar.setOnClickListener { finish() }

        cargarDatosConsultorio(idOffice)

        binding.btnConfirmarCita.setOnClickListener {

            procesarReserva(idOffice, idDoctor)
        }
    }

    private fun cargarDatosConsultorio(idOffice: String) {
        lifecycleScope.launch(Dispatchers.Main) {
            val result = withContext(Dispatchers.IO) { repository.getOfficeById(idOffice) }
            result.onSuccess { office ->
                if (office != null) {
                    currentOffice = office
                    binding.txtEspecialidadDoctor.text = office.specialty
                    configurarCalendario()
                } else {
                    Toast.makeText(this@ReservarCita, "Consultorio no encontrado", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    private fun configurarCalendario() {
        binding.calendarCita.minDate = System.currentTimeMillis() - 1000

        binding.calendarCita.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val calendar = Calendar.getInstance().apply { set(year, month, dayOfMonth) }
            val diaSeleccionadoStr = diasSemanaMap[calendar.get(Calendar.DAY_OF_WEEK)]

            val diasDisponibles = currentOffice?.availableDays ?: emptyList()

            if (diasDisponibles.contains(diaSeleccionadoStr)) {
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                selectedDate = sdf.format(calendar.time)
                selectedTime = ""
                generarCuadrosDeHora(currentOffice!!.openingTime, currentOffice!!.closingTime)
            } else {
                selectedDate = ""
                binding.layoutTimeSlots.removeAllViews()
                Toast.makeText(this, "El médico no atiende los días $diaSeleccionadoStr", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun generarCuadrosDeHora(opening: String, closing: String) {
        binding.layoutTimeSlots.removeAllViews()
        selectedTimeButton = null

        try {
            val sdf = SimpleDateFormat("hh:mm a", Locale.US)

            val calOpen = Calendar.getInstance().apply { time = sdf.parse(opening)!! }
            val calClose = Calendar.getInstance().apply { time = sdf.parse(closing)!! }

            while (calOpen.before(calClose)) {
                val horaStr = sdf.format(calOpen.time)
                crearBotonHora(horaStr)
                calOpen.add(Calendar.MINUTE, 30)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error calculando horarios de atención", Toast.LENGTH_SHORT).show()
        }
    }

    private fun crearBotonHora(hora: String) {
        val btn = Button(this).apply {
            text = hora
            setBackgroundColor(Color.TRANSPARENT)
            setTextColor(Color.parseColor("#00695C"))

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, 16, 0)
            layoutParams = params

            setOnClickListener {
                selectedTimeButton?.setBackgroundColor(Color.TRANSPARENT)
                selectedTimeButton?.setTextColor(Color.parseColor("#00695C"))

                setBackgroundColor(Color.parseColor("#00695C"))
                setTextColor(Color.WHITE)

                selectedTimeButton = this
                selectedTime = hora
            }
        }
        binding.layoutTimeSlots.addView(btn)
    }

    private fun procesarReserva(idOffice: String, idDoctor: String) {
        val uidPaciente = auth.currentUser?.uid
        if (uidPaciente == null) {
            Toast.makeText(this, "Error: Inicia sesión nuevamente", Toast.LENGTH_SHORT).show()
            return
        }

        val reason = binding.edtMotivo.text.toString().trim()
        val description = binding.edtDescripcion.text.toString().trim()

        if (reason.isEmpty()) {
            binding.edtMotivo.error = "Escribe el motivo de la cita"
            return
        }
        if (selectedDate.isEmpty() || selectedTime.isEmpty()) {
            Toast.makeText(this, "Por favor, selecciona una fecha y hora disponibles", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnConfirmarCita.isEnabled = false
        binding.btnConfirmarCita.text = "Comprobando disponibilidad..."

        lifecycleScope.launch(Dispatchers.Main) {
            val isTakenResult = withContext(Dispatchers.IO) {
                repository.checkIsSlotTaken(idOffice, selectedDate, selectedTime)
            }

            isTakenResult.onSuccess { isTaken ->

                if (isTaken) {
                    Toast.makeText(this@ReservarCita, "Ese horario acaba de ser reservado por alguien más. Por favor elige otro.", Toast.LENGTH_LONG).show()
                    binding.btnConfirmarCita.isEnabled = true
                    binding.btnConfirmarCita.text = "Confirmar cita"
                    return@onSuccess
                }

                // 1. Obtener datos para denormalización
                val appointmentData = withContext(Dispatchers.IO) {
                    runCatching {
                        coroutineScope {
                            val pacienteTask = async { repository.getPacienteById(uidPaciente).getOrNull() }
                            val doctorTask = async { repository.getDoctorById(idDoctor).getOrNull() }
                            val centerTask = async { 
                                currentOffice?.idCenter?.let { repository.getCenterById(it).getOrNull() }
                            }
                            
                            val p = pacienteTask.await()
                            val d = doctorTask.await()
                            val c = centerTask.await()
                            
                            Triple(p, d, c)
                        }
                    }.getOrNull()
                }

                val p = appointmentData?.first
                val d = appointmentData?.second
                val c = appointmentData?.third

                val nuevaCita = AppointmentDtoRemote(
                    idUser = uidPaciente,
                    idDoctor = idDoctor,
                    idOffice = idOffice,
                    idCenter = currentOffice?.idCenter ?: "",
                    patientName = if (p != null) "${p.info.nombres} ${p.info.apellidos}".trim() else "",
                    patientPhone = p?.info?.telefono ?: "",
                    doctorName = if (d != null) "Dr. ${d.info.nombres} ${d.info.apellidos}".trim() else binding.txtNombreDoctor.text.toString(),
                    centerName = c?.name ?: "",
                    reason = reason,
                    description = description,
                    date = selectedDate,
                    time = selectedTime,
                    status = "Pendiente",
                    diagnosis = null
                )

                val saveResult = withContext(Dispatchers.IO) { repository.saveAppointment(nuevaCita) }

                saveResult.onSuccess {
                    val intent = Intent(this@ReservarCita, PrincipalUser::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    intent.putExtra("MENSAJE_SNACKBAR", "Cita creada exitosamente")
                    startActivity(intent)
                    finish()
                }.onFailure { error ->
                    Toast.makeText(this@ReservarCita, "Error al guardar: ${error.message}", Toast.LENGTH_LONG).show()
                    binding.btnConfirmarCita.isEnabled = true
                    binding.btnConfirmarCita.text = "Confirmar cita"
                }

            }.onFailure { error ->
                Toast.makeText(this@ReservarCita, "Error consultando disponibilidad: ${error.message}", Toast.LENGTH_LONG).show()
                binding.btnConfirmarCita.isEnabled = true
                binding.btnConfirmarCita.text = "Confirmar cita"
            }
        }
    }
}