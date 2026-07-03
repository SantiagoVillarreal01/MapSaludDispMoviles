package ec.edu.mapsalud.userPages

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import ec.edu.mapsalud.databinding.UserReservarCitaBinding
import android.graphics.Color
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import ec.edu.mapsalud.dto.AppointmentDtoRemote
import ec.edu.mapsalud.dto.OfficeDtoRemote
import ec.edu.mapsalud.remote.impl.CitaRepositoryImpl
import ec.edu.mapsalud.remote.inter.CitaRepository
import com.google.firebase.auth.FirebaseAuth
import ec.edu.mapsalud.remote.impl.ConsultorioRepositoryImpl
import ec.edu.mapsalud.usercases.citasUC.CheckIsSlotTakenUC
import ec.edu.mapsalud.usercases.citasUC.SaveAppointmentUC
import ec.edu.mapsalud.usercases.consultoriosUC.GetOfficeByIdUC
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import ec.edu.mapsalud.utils.ThemeUtils
import ec.edu.mapsalud.viewmodel.CitaViewModel
import ec.edu.mapsalud.viewmodel.ConsultorioViewModel

class ReservarCita : AppCompatActivity() {

    private lateinit var binding: UserReservarCitaBinding
    private val citaVM by viewModels<CitaViewModel>()
    private val consultorioVM by viewModels<ConsultorioViewModel>()

    private val citaRepository = CitaRepositoryImpl()
    private val consultorioRepository = ConsultorioRepositoryImpl()
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

        initObservers()
        cargarDatosConsultorio(idOffice)

        binding.btnConfirmarCita.setOnClickListener {
            procesarReserva(idOffice, idDoctor)
        }
    }

    private fun cargarDatosConsultorio(idOffice: String) {
        consultorioVM.obtenerConsultorioPorId(
            idOffice = idOffice,
            getOfficeByIdUC = GetOfficeByIdUC(consultorioRepository)
        )
    }

    private fun initObservers() {
        consultorioVM.selectedOffice.observe(this) { office ->
            if (office != null) {
                currentOffice = office
                binding.txtEspecialidadDoctor.text = office.specialty
                configurarCalendario()
            } else {
                Toast.makeText(this, "Consultorio no encontrado", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        citaVM.operationSuccess.observe(this) { success ->
            if (success) {
                val intent = Intent(this, PrincipalUser::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                intent.putExtra("MENSAJE_SNACKBAR", "Cita creada exitosamente")
                startActivity(intent)
                finish()
            } else {
                binding.btnConfirmarCita.isEnabled = true
                binding.btnConfirmarCita.text = "Confirmar cita"
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
        val uidPaciente = auth.currentUser?.uid ?: return

        val reason = binding.edtMotivo.text.toString().trim()
        val description = binding.edtDescripcion.text.toString().trim()

        if (reason.isEmpty()) {
            binding.edtMotivo.error = "Escribe el motivo"
            return
        }
        if (selectedDate.isEmpty() || selectedTime.isEmpty()) {
            Toast.makeText(this, "Selecciona fecha y hora", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnConfirmarCita.isEnabled = false
        binding.btnConfirmarCita.text = "Comprobando disponibilidad..."

        citaVM.verificarDisponibilidad(idOffice, selectedDate, selectedTime,
            CheckIsSlotTakenUC(citaRepository)
        )

        citaVM.isSlotTaken.observe(this) { isTaken ->
            if (isTaken) {
                Toast.makeText(this, "Horario ocupado, elige otro", Toast.LENGTH_LONG).show()
                binding.btnConfirmarCita.isEnabled = true
                binding.btnConfirmarCita.text = "Confirmar cita"
                return@observe
            }
            lifecycleScope.launch {
                val nuevaCita = AppointmentDtoRemote(
                    idUser = uidPaciente,
                    idDoctor = idDoctor,
                    idOffice = idOffice,
                    idCenter = currentOffice?.idCenter ?: "",
                    // ... resto de mapeo, idealmente estos datos vendrían ya resueltos del VM
                    reason = reason,
                    description = description,
                    date = selectedDate,
                    time = selectedTime,
                    status = "Pendiente"
                )

                citaVM.agendarCita(nuevaCita, SaveAppointmentUC(citaRepository))
            }
        }
    }
}