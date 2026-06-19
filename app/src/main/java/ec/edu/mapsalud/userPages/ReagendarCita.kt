package ec.edu.mapsalud.userPages

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import ec.edu.mapsalud.databinding.UserReagendarCitaBinding
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import ec.edu.mapsalud.databinding.CuadroCitaPendienteBinding
import ec.edu.mapsalud.dto.AppointmentDetail
import ec.edu.mapsalud.dto.OfficeDtoRemote
import ec.edu.mapsalud.remote.impl.AppointmentRemoteImpl
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ReagendarCita : AppCompatActivity() {

    private lateinit var binding: UserReagendarCitaBinding
    private val appointmentRemote = AppointmentRemoteImpl()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: "ID_TEMP"

    private var selectedAppointmentDetail: AppointmentDetail? = null
    private var selectedDateStr: String? = null
    private var selectedTimeStr: String? = null

    private var selectedTimeButton: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = UserReagendarCitaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configurarUI()
        cargarCitasPendientes()
    }

    private fun configurarUI() {
        binding.btnRegresar.setOnClickListener { finish() }

        binding.decorCalendar.minDate = System.currentTimeMillis()
        binding.decorCalendar.isEnabled = false

        binding.decorCalendar.setOnDateChangeListener { _, year, month, dayOfMonth ->
            if (selectedAppointmentDetail == null) {
                Toast.makeText(this, "Por favor, selecciona primero la cita que deseas modificar de la lista superior.", Toast.LENGTH_SHORT).show()
                return@setOnDateChangeListener
            }

            val fechaSeleccionada = String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year)
            val diaSemana = obtenerDiaSemana(year, month, dayOfMonth)

            selectedDateStr = fechaSeleccionada
            selectedTimeStr = null
            selectedTimeButton = null

            verificarDisponibilidadDia(diaSemana, fechaSeleccionada)
        }

        binding.btnReagendarCita.setOnClickListener { confirmarReagendamiento() }
    }

    private fun cargarCitasPendientes() {
        lifecycleScope.launch {
            val result = appointmentRemote.getPendingAppointmentsWithDetails(currentUserId)

            result.onSuccess { citas ->
                binding.containerCitas.removeAllViews()

                citas.forEach { detalle ->
                    val cardBinding = CuadroCitaPendienteBinding.inflate(
                        LayoutInflater.from(this@ReagendarCita),
                        binding.containerCitas,
                        false
                    )

                    // 1. CORRECCIÓN: Acceso correcto a la subclase 'info' y formato limpio
                    val primerNombre = detalle.doctor.info.nombres.split(" ").firstOrNull() ?: ""
                    val primerApellido = detalle.doctor.info.apellidos.split(" ").firstOrNull() ?: ""

                    cardBinding.txtNombreMedico.text = "Dr. $primerNombre $primerApellido"
                    cardBinding.txtEspecialidad.text = detalle.office.specialty
                    cardBinding.txtFechaHora.text = "📅 ${detalle.appointment.date} • 🕒 ${detalle.appointment.time}"

                    cardBinding.root.setOnClickListener {
                        seleccionarCita(detalle)
                    }

                    binding.containerCitas.addView(cardBinding.root)
                }
            }.onFailure {
                Toast.makeText(this@ReagendarCita, "Error al cargar citas", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun seleccionarCita(detalle: AppointmentDetail) {
        selectedAppointmentDetail = detalle
        binding.decorCalendar.isEnabled = true
        binding.containerHorarios.removeAllViews()

        val originalDateStr = detalle.appointment.date
        val originalTimeStr = detalle.appointment.time

        selectedDateStr = originalDateStr
        selectedTimeStr = originalTimeStr
        selectedTimeButton = null

        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        try {
            val date = sdf.parse(originalDateStr)
            if (date != null) {
                binding.decorCalendar.date = date.time
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val diaSemana = obtenerDiaSemanaDesdeString(originalDateStr)
        verificarDisponibilidadDia(diaSemana, originalDateStr)

        Toast.makeText(this, "Horario actual cargado", Toast.LENGTH_SHORT).show()
    }

    private fun verificarDisponibilidadDia(diaSemana: String, fechaCompleta: String) {
        val office = selectedAppointmentDetail?.office ?: return

        if (!office.availableDays.contains(diaSemana)) {
            binding.containerHorarios.removeAllViews()
            Toast.makeText(this, "El médico no atiende los $diaSemana", Toast.LENGTH_SHORT).show()
            return
        }

        generarYValidarHorarios(office, fechaCompleta)
    }

    private fun generarYValidarHorarios(office: OfficeDtoRemote, fecha: String) {
        binding.containerHorarios.removeAllViews()

        val slots = calcularIntervalos(office.openingTime, office.closingTime)

        val isOriginalDate = (fecha == selectedAppointmentDetail?.appointment?.date)
        val originalTime = selectedAppointmentDetail?.appointment?.time

        lifecycleScope.launch {

            val resultados = slots.map { horaPropuesta ->
                async {
                    val isOwnCurrentSlot = isOriginalDate && (horaPropuesta == originalTime)

                    val isTaken = if (isOwnCurrentSlot) {
                        false
                    } else {
                        appointmentRemote.checkIsSlotTaken(office.id, fecha, horaPropuesta).getOrDefault(true)
                    }
                    Triple(horaPropuesta, !isTaken, isOwnCurrentSlot)
                }
            }.awaitAll()

            resultados.forEach { (hora, isAvailable, isPreselected) ->
                agregarBotonHorario(hora, isAvailable, isPreselected)
            }
        }
    }

    private fun agregarBotonHorario(hora: String, isAvailable: Boolean, isPreselected: Boolean = false) {
        val btn = Button(this).apply {
            text = hora
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 16, 0) }

            if (isAvailable) {
                setOnClickListener {
                    selectedTimeButton?.setBackgroundColor(Color.TRANSPARENT)
                    selectedTimeButton?.setTextColor(Color.parseColor("#00695C"))

                    setBackgroundColor(Color.parseColor("#00695C"))
                    setTextColor(Color.WHITE)

                    selectedTimeButton = this
                    selectedTimeStr = hora
                }

                if (isPreselected) {
                    setBackgroundColor(Color.parseColor("#00695C"))
                    setTextColor(Color.WHITE)
                    selectedTimeButton = this
                    selectedTimeStr = hora
                } else {
                    setBackgroundColor(Color.TRANSPARENT)
                    setTextColor(Color.parseColor("#00695C"))
                }

            } else {
                isEnabled = false
                alpha = 0.5f
                setBackgroundColor(Color.LTGRAY)
                setTextColor(Color.DKGRAY)
            }
        }
        binding.containerHorarios.addView(btn)
    }

    private fun confirmarReagendamiento() {
        val idCita = selectedAppointmentDetail?.appointment?.id

        if (idCita == null || selectedDateStr == null || selectedTimeStr == null) {
            Toast.makeText(this, "Por favor selecciona una cita, fecha y hora válidas", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnReagendarCita.isEnabled = false
        binding.btnReagendarCita.text = "Comprobando..."

        lifecycleScope.launch {
            val finalCheck = appointmentRemote.checkIsSlotTaken(
                selectedAppointmentDetail!!.office.id, selectedDateStr!!, selectedTimeStr!!
            ).getOrDefault(true)

            if (finalCheck) {
                Toast.makeText(this@ReagendarCita, "Lo sentimos, el horario acaba de ser ocupado", Toast.LENGTH_LONG).show()
                verificarDisponibilidadDia(obtenerDiaSemanaDesdeString(selectedDateStr!!), selectedDateStr!!)

                binding.btnReagendarCita.isEnabled = true
                binding.btnReagendarCita.text = "Reagendar cita"
                return@launch
            }

            appointmentRemote.updateAppointment(idCita, selectedDateStr!!, selectedTimeStr!!)
                .onSuccess {
                    val intent = Intent(this@ReagendarCita, PrincipalUser::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        putExtra("MENSAJE_SNACKBAR", "Cita reagendada con éxito")
                    }
                    startActivity(intent)
                    finish()
                }.onFailure {
                    Toast.makeText(this@ReagendarCita, "Error al actualizar la base de datos", Toast.LENGTH_SHORT).show()
                    binding.btnReagendarCita.isEnabled = true
                    binding.btnReagendarCita.text = "Reagendar cita"
                }
        }
    }

    private fun calcularIntervalos(apertura: String, cierre: String): List<String> {
        val slots = mutableListOf<String>()
        try {
            var actual = parseTimeMin(apertura)
            val final = parseTimeMin(cierre)

            while (actual < final) {
                slots.add(formatTimeMin(actual))
                actual += 30
            }
        } catch (e: Exception) { }
        return slots
    }

    private fun parseTimeMin(time: String): Int {
        return try {
            val cleanTime = time.trim().uppercase()

            if (cleanTime.contains("AM") || cleanTime.contains("PM")) {
                val isPM = cleanTime.contains("PM")
                val isAM = cleanTime.contains("AM")

                val timeParts = cleanTime.replace("AM", "").replace("PM", "").trim().split(":")
                var hours = timeParts[0].toInt()
                val minutes = timeParts[1].toInt()

                if (isPM && hours < 12) hours += 12
                if (isAM && hours == 12) hours = 0

                hours * 60 + minutes
            } else {
                val parts = time.split(":")
                parts[0].toInt() * 60 + parts[1].toInt()
            }
        } catch (e: Exception) {
            0
        }
    }

    private fun formatTimeMin(min: Int): String {
        return String.format(Locale.getDefault(), "%02d:%02d", min / 60, min % 60)
    }

    private fun obtenerDiaSemana(year: Int, month: Int, day: Int): String {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, day)
        return traducirDia(calendar.get(Calendar.DAY_OF_WEEK))
    }

    private fun obtenerDiaSemanaDesdeString(fecha: String): String {
        val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val date = format.parse(fecha) ?: return ""
        val calendar = Calendar.getInstance().apply { time = date }
        return traducirDia(calendar.get(Calendar.DAY_OF_WEEK))
    }

    private fun traducirDia(dayOfWeek: Int): String {
        return when (dayOfWeek) {
            Calendar.MONDAY -> "Lunes"
            Calendar.TUESDAY -> "Martes"
            Calendar.WEDNESDAY -> "Miércoles"
            Calendar.THURSDAY -> "Jueves"
            Calendar.FRIDAY -> "Viernes"
            Calendar.SATURDAY -> "Sábado"
            Calendar.SUNDAY -> "Domingo"
            else -> ""
        }
    }
}