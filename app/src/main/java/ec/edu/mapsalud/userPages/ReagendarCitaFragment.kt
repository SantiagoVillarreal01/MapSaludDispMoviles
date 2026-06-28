package ec.edu.mapsalud.userPages

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import ec.edu.mapsalud.R
import ec.edu.mapsalud.databinding.UserReagendarCitaBinding
import ec.edu.mapsalud.dto.AppointmentDetail
import ec.edu.mapsalud.remote.impl.AppointmentRemoteImpl
import ec.edu.mapsalud.remote.inter.AppointmentRemote
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import android.view.LayoutInflater
import android.view.ViewGroup
import ec.edu.mapsalud.databinding.CuadroCitaCompletaBinding

class ReagendarCitaFragment : Fragment(R.layout.user_reagendar_cita) {

    private var _binding: UserReagendarCitaBinding? = null
    private val binding get() = _binding!!
    private val appointmentRemote: AppointmentRemote = AppointmentRemoteImpl()
    private val auth = FirebaseAuth.getInstance()

    private var selectedAppointment: AppointmentDetail? = null
    private var selectedDate: String = ""
    private var selectedTime: String = ""
    private var selectedTimeButton: Button? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = UserReagendarCitaBinding.bind(view)

        configurarCalendario()
        cargarCitasPendientes()

        binding.btnReagendarCita.setOnClickListener {
            procesarReagendamiento()
        }
    }

    private fun cargarCitasPendientes() {
        val userId = auth.currentUser?.uid ?: return
        lifecycleScope.launch(Dispatchers.Main) {
            val result = withContext(Dispatchers.IO) {
                appointmentRemote.fetchAppointmentsWithDetails(userId, "Pendiente")
            }
            result.onSuccess { list ->
                poblarCitas(list)
            }.onFailure {
                Toast.makeText(requireContext(), "Error al cargar citas", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun poblarCitas(list: List<AppointmentDetail>) {
        binding.containerCitas.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())
        
        list.forEach { item ->
            val itemBinding = CuadroCitaCompletaBinding.inflate(inflater, binding.containerCitas, false)
            itemBinding.txtNombrePaciente.text = "Dr. ${item.doctor.info.nombres} ${item.doctor.info.apellidos}"
            itemBinding.txtFechaCita.text = "${item.appointment.date} - ${item.appointment.time}"
            itemBinding.txtMotivo.text = item.appointment.reason
            
            itemBinding.root.setOnClickListener {
                selectedAppointment = item
                Toast.makeText(requireContext(), "Cita seleccionada", Toast.LENGTH_SHORT).show()
                // Resetear seleccion de hora si cambia la cita
                selectedTime = ""
                binding.containerHorarios.removeAllViews()
            }
            
            binding.containerCitas.addView(itemBinding.root)
        }
    }

    private fun configurarCalendario() {
        binding.decorCalendar.minDate = System.currentTimeMillis()
        binding.decorCalendar.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val calendar = Calendar.getInstance().apply { set(year, month, dayOfMonth) }
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            selectedDate = sdf.format(calendar.time)
            
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            val diaSemana = when(dayOfWeek) {
                Calendar.MONDAY -> "Lunes"
                Calendar.TUESDAY -> "Martes"
                Calendar.WEDNESDAY -> "Miércoles"
                Calendar.THURSDAY -> "Jueves"
                Calendar.FRIDAY -> "Viernes"
                Calendar.SATURDAY -> "Sábado"
                Calendar.SUNDAY -> "Domingo"
                else -> ""
            }

            selectedAppointment?.let {
                if (it.office.availableDays.contains(diaSemana)) {
                    generarCuadrosDeHora(it.office.openingTime, it.office.closingTime)
                } else {
                    binding.containerHorarios.removeAllViews()
                    Toast.makeText(requireContext(), "El médico no atiende los $diaSemana", Toast.LENGTH_SHORT).show()
                }
            } ?: run {
                Toast.makeText(requireContext(), "Por favor selecciona primero una cita", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun generarCuadrosDeHora(opening: String, closing: String) {
        binding.containerHorarios.removeAllViews()
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
        }
    }

    private fun crearBotonHora(hora: String) {
        val btn = Button(requireContext()).apply {
            text = hora
            setBackgroundColor(Color.TRANSPARENT)
            setTextColor(Color.parseColor("#00695C"))
            val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
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
        binding.containerHorarios.addView(btn)
    }

    private fun procesarReagendamiento() {
        val appointment = selectedAppointment
        if (appointment == null || selectedDate.isEmpty() || selectedTime.isEmpty()) {
            Toast.makeText(requireContext(), "Por favor selecciona una cita, fecha y hora válidas", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.Main) {
            val result = withContext(Dispatchers.IO) {
                appointmentRemote.updateAppointment(appointment.appointment.id, selectedDate, selectedTime)
            }
            result.onSuccess {
                Toast.makeText(requireContext(), "Cita reagendada exitosamente", Toast.LENGTH_SHORT).show()
                cargarCitasPendientes()
                binding.containerHorarios.removeAllViews()
                selectedAppointment = null
                selectedDate = ""
                selectedTime = ""
            }.onFailure {
                Toast.makeText(requireContext(), "Error al reagendar", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
