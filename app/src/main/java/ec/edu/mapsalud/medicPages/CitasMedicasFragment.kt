package ec.edu.mapsalud.medicPages

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import ec.edu.mapsalud.R
import ec.edu.mapsalud.databinding.MedicCuadroCitaBinding
import ec.edu.mapsalud.databinding.MedicFragmentCitasBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.widget.Toast
import ec.edu.mapsalud.dto.AppointmentDtoRemote
import ec.edu.mapsalud.dto.AppointmentPaciente
import ec.edu.mapsalud.dto.Paciente
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.collections.emptyList

class CitasMedicasFragment : Fragment(R.layout.medic_fragment_citas) {

    private var _binding: MedicFragmentCitasBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val currentDoctorId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = MedicFragmentCitasBinding.bind(view)

        binding.recyclerCitasHoy.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerCitasManana.layoutManager = LinearLayoutManager(requireContext())

        cargarCitas()
    }

    private fun cargarCitas() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val cal = Calendar.getInstance()
                val fechaHoy = dateFormat.format(cal.time)
                cal.add(Calendar.DAY_OF_YEAR, 1)
                val fechaManana = dateFormat.format(cal.time)

                val officesSnapshot = db.collection("consultorios")
                    .whereEqualTo("idDoctor", currentDoctorId)
                    .get().await()

                val officeIds = officesSnapshot.documents.map { it.id }

                if (officeIds.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        actualizarUI(emptyList(), emptyList())
                    }
                    return@launch
                }

                val citasSnapshot = db.collection("citas")
                    .whereIn("idOffice", officeIds)
                    .whereEqualTo("status", "Pendiente")
                    .get().await()

                val todasLasCitas = citasSnapshot.toObjects(AppointmentDtoRemote::class.java)

                val citasHoy = mutableListOf<AppointmentPaciente>()
                val citasManana = mutableListOf<AppointmentPaciente>()

                for (cita in todasLasCitas) {
                    if (cita.date == fechaHoy || cita.date == fechaManana) {
                        // 1. Intentar usar datos denormalizados
                        val citaUI = if (cita.patientName.isNotEmpty()) {
                            val (horaFormat, amPm) = formatearHora(cita.time)
                            // Creamos un Paciente ficticio para el DTO AppointmentPaciente si es necesario, 
                            // o mejor adaptamos AppointmentPaciente
                            val dummyPaciente = Paciente(info = ec.edu.mapsalud.dto.UsuarioInfo(
                                nombres = cita.patientName,
                                apellidos = ""
                            ))
                            AppointmentPaciente(cita, dummyPaciente, horaFormat, amPm)
                        } else {
                            // 2. Fallback: Consultar base de datos (para citas antiguas)
                            val pacienteDoc = db.collection("usuarios").document(cita.idUser).get().await()
                            val paciente = pacienteDoc.toObject(Paciente::class.java)
                            if (paciente != null) {
                                val (horaFormat, amPm) = formatearHora(cita.time)
                                AppointmentPaciente(cita, paciente, horaFormat, amPm)
                            } else null
                        }

                        if (citaUI != null) {
                            if (cita.date == fechaHoy) citasHoy.add(citaUI)
                            else citasManana.add(citaUI)
                        }
                    }
                }
                citasHoy.sortBy { it.appointment.time }
                citasManana.sortBy { it.appointment.time }

                // 6. Enviar a la UI en el hilo principal
                withContext(Dispatchers.Main) {
                    actualizarUI(citasHoy, citasManana)
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error al cargar citas: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun actualizarUI(hoy: List<AppointmentPaciente>, manana: List<AppointmentPaciente>) {
        binding.recyclerCitasHoy.adapter = CitasMedicAdapter(hoy) { citaSeleccionada ->
            abrirDetalleCita(citaSeleccionada)
        }
        binding.recyclerCitasManana.adapter = CitasMedicAdapter(manana) { citaSeleccionada ->
            abrirDetalleCita(citaSeleccionada)
        }
    }

    private fun formatearHora(time24: String): Pair<String, String> {
        return try {
            val parts = time24.split(":")
            var hour = parts[0].toInt()
            val min = parts[1]
            val amPm = if (hour >= 12) "PM" else "AM"
            if (hour > 12) hour -= 12
            if (hour == 0) hour = 12
            Pair(String.format(Locale.getDefault(), "%02d:%s", hour, min), amPm)
        } catch (e: Exception) {
            Pair(time24, "")
        }
    }

    private fun abrirDetalleCita(cita: AppointmentPaciente) {
        val intent = Intent(requireContext(), DetalleCitaMedic::class.java)
        // Pasamos el ID de la cita para que la otra ventana pueda gestionarla o agregar el diagnóstico
        intent.putExtra("ID_CITA", cita.appointment.id)
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class CitasMedicAdapter(
        private val citas: List<AppointmentPaciente>,
        private val onCitaClick: (AppointmentPaciente) -> Unit
    ) : RecyclerView.Adapter<CitasMedicAdapter.ViewHolder>() {

        inner class ViewHolder(val itemBinding: MedicCuadroCitaBinding) : RecyclerView.ViewHolder(itemBinding.root) {
            fun bind(item: AppointmentPaciente) {
                itemBinding.txtHoraCita.text = item.horaFormateada
                itemBinding.txtAmPmCita.text = item.amPm
                itemBinding.txtNombrePaciente.text = "${item.paciente.info.nombres} ${item.paciente.info.apellidos}"
                itemBinding.txtMotivoCita.text = item.appointment.reason

                itemBinding.root.setOnClickListener {
                    onCitaClick(item)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
            MedicCuadroCitaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
        override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(citas[position])
        override fun getItemCount() = citas.size
    }
}