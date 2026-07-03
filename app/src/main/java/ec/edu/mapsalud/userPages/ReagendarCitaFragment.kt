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
import ec.edu.mapsalud.remote.impl.CitaRepositoryImpl
import ec.edu.mapsalud.remote.inter.CitaRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import android.view.LayoutInflater
import androidx.fragment.app.viewModels
import ec.edu.mapsalud.databinding.CuadroCitaCompletaBinding
import ec.edu.mapsalud.remote.impl.ConsultorioRepositoryImpl
import ec.edu.mapsalud.remote.impl.UsuariosRepositoryImpl
import ec.edu.mapsalud.usercases.citasUC.CheckIsSlotTakenUC
import ec.edu.mapsalud.usercases.citasUC.FetchAppointmentsWithDetailsUC
import ec.edu.mapsalud.usercases.citasUC.UpdateAppointmentUC
import ec.edu.mapsalud.viewmodel.CitaViewModel

class ReagendarCitaFragment : Fragment(R.layout.user_reagendar_cita) {

    private var _binding: UserReagendarCitaBinding? = null
    private val binding get() = _binding!!

    private val citaVM by viewModels<CitaViewModel>()
    private val citaRepository = CitaRepositoryImpl()
    private val consultorioRepository = ConsultorioRepositoryImpl()
    private val usuarioRepository = UsuariosRepositoryImpl()
    private val auth = FirebaseAuth.getInstance()

    private var selectedAppointment: AppointmentDetail? = null
    private var selectedDate: String = ""
    private var selectedTime: String = ""
    private var selectedTimeButton: Button? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = UserReagendarCitaBinding.bind(view)

        configurarCalendario()
        initObservers()
        cargarCitasPendientes()

        binding.btnReagendarCita.setOnClickListener {
            procesarReagendamiento()
        }
    }

    private fun cargarCitasPendientes() {
        val userId = auth.currentUser?.uid ?: return
        citaVM.cargarCitasConDetalles(
            userId = userId,
            status = "Pendiente",
            fetchDetailsUC = FetchAppointmentsWithDetailsUC(
                citaRepo = citaRepository,
                consultorioRepo = consultorioRepository,
                usuarioRepo = usuarioRepository
            )
        )
    }

    private fun initObservers() {
        citaVM.appointmentsDetails.observe(viewLifecycleOwner) { list ->
            poblarCitas(list)
        }
        citaVM.operationSuccess.observe(viewLifecycleOwner) { exito ->
            binding.btnReagendarCita.isEnabled = true
            if (exito) {
                Toast.makeText(requireContext(), "Cita reagendada exitosamente", Toast.LENGTH_SHORT).show()
                cargarCitasPendientes()
                binding.containerHorarios.removeAllViews()
                selectedAppointment = null
                selectedDate = ""
                selectedTime = ""
            } else {
                Toast.makeText(requireContext(), "Error al reagendar", Toast.LENGTH_SHORT).show()
            }
        }

        citaVM.isSlotTaken.observe(viewLifecycleOwner) { ocupado ->
            val appointment = selectedAppointment
            if (appointment == null || selectedDate.isEmpty() || selectedTime.isEmpty()) return@observe

            if (ocupado) {
                Toast.makeText(requireContext(), "El horario seleccionado ya está ocupado", Toast.LENGTH_LONG).show()
                binding.btnReagendarCita.isEnabled = true
            } else {
                citaVM.reprogramarCita(
                    appointmentId = appointment.appointment.id,
                    newDate = selectedDate,
                    newTime = selectedTime,
                    updateUC = UpdateAppointmentUC(citaRepository)
                )
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

        binding.btnReagendarCita.isEnabled = false

        citaVM.verificarDisponibilidad(
            idOffice = appointment.office.id,
            date = selectedDate,
            time = selectedTime,
            checkUC = CheckIsSlotTakenUC(citaRepository)
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
