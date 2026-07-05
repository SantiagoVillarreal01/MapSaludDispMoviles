package ec.edu.mapsalud.patientPages

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import ec.edu.mapsalud.databinding.UserReservarCitaBinding
import android.graphics.Color
import android.speech.RecognizerIntent
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import ec.edu.mapsalud.dto.CitaDtoRemote
import ec.edu.mapsalud.dto.ConsultorioDtoRemote
import ec.edu.mapsalud.remote.impl.CitaRepositoryImpl
import com.google.firebase.auth.FirebaseAuth
import ec.edu.mapsalud.R
import ec.edu.mapsalud.remote.impl.ConsultorioRepositoryImpl
import ec.edu.mapsalud.remote.impl.UsuariosRepositoryImpl
import ec.edu.mapsalud.usercases.citasUC.CheckIsSlotTakenUC
import ec.edu.mapsalud.usercases.citasUC.SaveAppointmentUC
import ec.edu.mapsalud.usercases.consultoriosUC.GetOfficeByIdUC
import ec.edu.mapsalud.utils.ThemeUtils
import ec.edu.mapsalud.viewmodel.CitaViewModel
import ec.edu.mapsalud.viewmodel.ConsultorioViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReservarCita : AppCompatActivity() {

    private lateinit var binding: UserReservarCitaBinding
    private val citaVM by viewModels<CitaViewModel>()
    private val consultorioVM by viewModels<ConsultorioViewModel>()

    private val citaRepository = CitaRepositoryImpl()
    private val consultorioRepository = ConsultorioRepositoryImpl()

    private val usuarioRepository = UsuariosRepositoryImpl()
    private val auth = FirebaseAuth.getInstance()

    private var currentOffice: ConsultorioDtoRemote? = null
    private var selectedDate: String = ""
    private var selectedTime: String = ""
    private var selectedTimeButton: Button? = null

    private var idOffice: String = ""
    private var idDoctor: String = ""

    private var idCampoObjetivo: Int = 0

    private val diasSemanaMap = mapOf(
        Calendar.SUNDAY to "Domingo", Calendar.MONDAY to "Lunes",
        Calendar.TUESDAY to "Martes", Calendar.WEDNESDAY to "Miércoles",
        Calendar.THURSDAY to "Jueves", Calendar.FRIDAY to "Viernes",
        Calendar.SATURDAY to "Sábado"
    )

    private val speechLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { resultado ->
        if (resultado.resultCode == Activity.RESULT_OK && resultado.data != null) {
            val palabrasReconocidas = resultado.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!palabrasReconocidas.isNullOrEmpty()) {
                val textoDictado = palabrasReconocidas[0]

                when (idCampoObjetivo) {
                    binding.edtMotivo.id -> {
                        binding.edtMotivo.setText(textoDictado)
                        binding.edtMotivo.setSelection(textoDictado.length) // Mover cursor al final
                    }
                    binding.edtDescripcion.id -> {
                        binding.edtDescripcion.setText(textoDictado)
                        binding.edtDescripcion.setSelection(textoDictado.length)
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeUtils.applyTheme(this)
        super.onCreate(savedInstanceState)
        binding = UserReservarCitaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val doctorName = intent.getStringExtra("NOMBRE_DOCTOR") ?: ""
        idOffice = intent.getStringExtra("ID_OFFICE") ?: ""
        idDoctor = intent.getStringExtra("ID_DOCTOR") ?: ""

        val fotoUrlDoctor = intent.getStringExtra("URL_FOTO_DOCTOR") ?: ""

        binding.txtNombreDoctor.text = doctorName
        binding.btnRegresar.setOnClickListener { finish() }

        Glide.with(this).clear(binding.imgDoctorReserva)
        if (fotoUrlDoctor.isNotEmpty()) {
            Glide.with(this)
                .load(fotoUrlDoctor)
                .centerCrop()
                .placeholder(R.drawable.user)
                .error(R.drawable.user)
                .into(binding.imgDoctorReserva)
        } else {
            binding.imgDoctorReserva.setImageResource(R.drawable.user)
        }

        configurarBotonesDeDictado()
        initObservers()
        cargarDatosConsultorio(idOffice)

        binding.btnConfirmarCita.setOnClickListener {
            procesarReserva()
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
                val intent = Intent(this, PrincipalPatient::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                intent.putExtra("MENSAJE_SNACKBAR", "Cita creada exitosamente")
                startActivity(intent)
                finish()
            } else {
                binding.btnConfirmarCita.isEnabled = true
                binding.btnConfirmarCita.text = "Confirmar cita"
            }
        }

        citaVM.isSlotTaken.observe(this) { isTaken ->
            if (binding.btnConfirmarCita.isEnabled) return@observe

            if (isTaken) {
                Toast.makeText(this, "Horario ocupado, elige otro", Toast.LENGTH_LONG).show()
                binding.btnConfirmarCita.isEnabled = true
                binding.btnConfirmarCita.text = "Confirmar cita"
            } else {
                val uidPaciente = auth.currentUser?.uid ?: return@observe
                val reason = binding.edtMotivo.text.toString().trim()
                val description = binding.edtDescripcion.text.toString().trim()
                val idOffice = intent.getStringExtra("ID_OFFICE") ?: ""
                val idDoctor = intent.getStringExtra("ID_DOCTOR") ?: ""

                val nuevaCita = CitaDtoRemote(
                    idUser = uidPaciente,
                    idDoctor = idDoctor,
                    idOffice = idOffice,
                    idCenter = currentOffice?.idCenter ?: "",
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

    private fun configurarBotonesDeDictado() {
        binding.tilMotivo.setEndIconOnClickListener {
            iniciarDictadoPorVoz(binding.edtMotivo.id)
        }
        binding.tilDescripcion.setEndIconOnClickListener {
            iniciarDictadoPorVoz(binding.edtDescripcion.id)
        }
    }

    private fun iniciarDictadoPorVoz(targetEditTextId: Int) {
        idCampoObjetivo = targetEditTextId

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault()) // Usa el idioma del teléfono del usuario
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Te escuchamos. Empieza a hablar...")
        }

        try {
            speechLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Tu dispositivo no soporta reconocimiento de voz nativo", Toast.LENGTH_SHORT).show()
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

    private fun procesarReserva() {
        val uidPaciente = auth.currentUser?.uid ?: return
        val reason = binding.edtMotivo.text.toString().trim()

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

        citaVM.verificarDisponibilidad(
            idOffice, selectedDate, selectedTime,
            CheckIsSlotTakenUC(citaRepository)
        )
    }
}