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
import androidx.recyclerview.widget.DiffUtil
import com.google.firebase.auth.FirebaseAuth
import ec.edu.mapsalud.R
import ec.edu.mapsalud.databinding.MedicCuadroCitaBinding
import ec.edu.mapsalud.databinding.MedicFragmentCitasBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.AsyncListDiffer
import ec.edu.mapsalud.dto.CitaPaciente
import ec.edu.mapsalud.dto.Paciente
import ec.edu.mapsalud.remote.impl.CitaRepositoryImpl
import ec.edu.mapsalud.remote.impl.ConsultorioRepositoryImpl
import ec.edu.mapsalud.remote.impl.UsuariosRepositoryImpl
import ec.edu.mapsalud.usercases.citasUC.GetPendingAppointmentsByOfficesUC
import ec.edu.mapsalud.usercases.consultoriosUC.GetOfficesByDoctorUC
import ec.edu.mapsalud.viewmodel.CitaViewModel
import ec.edu.mapsalud.viewmodel.ConsultorioViewModel
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.collections.emptyList

class CitasMedicasFragment : Fragment(R.layout.medic_fragment_citas) {

    private var _binding: MedicFragmentCitasBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapterHoy: CitasMedicAdapter
    private lateinit var adapterManana: CitasMedicAdapter
    private val currentDoctorId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private val consultorioVM by viewModels<ConsultorioViewModel>()
    private val citaVM by viewModels<CitaViewModel>()
    private val consultorioRepository = ConsultorioRepositoryImpl()
    private val citaRepository = CitaRepositoryImpl()
    private val usuarioRepository = UsuariosRepositoryImpl()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = MedicFragmentCitasBinding.bind(view)

        configurarRecyclerViews()
        initObservers()
        cargarCitas()
    }

    private fun configurarRecyclerViews() {
        adapterHoy = CitasMedicAdapter { citaSeleccionada ->
            abrirDetalleCita(citaSeleccionada)
        }
        binding.recyclerCitasHoy.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerCitasHoy.adapter = adapterHoy

        binding.recyclerCitasHoy.isNestedScrollingEnabled = true

        adapterManana = CitasMedicAdapter { citaSeleccionada ->
            abrirDetalleCita(citaSeleccionada)
        }
        binding.recyclerCitasManana.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerCitasManana.adapter = adapterManana

        binding.recyclerCitasManana.isNestedScrollingEnabled = true
    }

    private fun cargarCitas() {
        if (currentDoctorId.isEmpty()) return
        consultorioVM.cargarConsultoriosPorDoctor(
            idDoctor = currentDoctorId,
            getOfficesByDoctorUC = GetOfficesByDoctorUC(consultorioRepository)
        )
    }

    private fun initObservers() {
        consultorioVM.officesList.observe(viewLifecycleOwner) { offices ->
            val officeIds = offices.map { it.id }
            if (officeIds.isEmpty()) {
                adapterHoy.submitList(emptyList())
                adapterManana.submitList(emptyList())
                return@observe
            }
            citaVM.cargarCitasPendientesPorConsultorios(
                officeIds = officeIds,
                getByOfficesUC = GetPendingAppointmentsByOfficesUC(citaRepository)
            )
        }

        citaVM.appointmentsRawList.observe(viewLifecycleOwner) { todasLasCitas ->
            lifecycleScope.launch(Dispatchers.Main) {
                try {
                    val listasFiltradas = withContext(Dispatchers.IO) {
                        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        val cal = Calendar.getInstance()
                        val fechaHoy = dateFormat.format(cal.time)
                        cal.add(Calendar.DAY_OF_YEAR, 1)
                        val fechaManana = dateFormat.format(cal.time)

                        val citasHoy = mutableListOf<CitaPaciente>()
                        val citasManana = mutableListOf<CitaPaciente>()

                        for (cita in todasLasCitas) {
                            if (cita.date == fechaHoy || cita.date == fechaManana) {
                                val paciente = if (cita.patientName.isNotEmpty()) {
                                    Paciente(info = ec.edu.mapsalud.dto.UsuarioInfo(
                                        nombres = cita.patientName,
                                        apellidos = ""
                                    ))
                                } else {
                                    usuarioRepository.getPacienteById(cita.idUser).getOrNull()
                                }

                                if (paciente != null) {
                                    val (horaFormat, amPm) = formatearHora(cita.time)
                                    val citaUI = CitaPaciente(cita, paciente, horaFormat, amPm)

                                    if (cita.date == fechaHoy) citasHoy.add(citaUI)
                                    else citasManana.add(citaUI)
                                }
                            }
                        }
                        citasHoy.sortBy { it.appointment.time }
                        citasManana.sortBy { it.appointment.time }
                        Pair(citasHoy, citasManana)
                    }
                    adapterHoy.submitList(listasFiltradas.first)
                    adapterManana.submitList(listasFiltradas.second)

                } catch (e: Exception) {
                    context?.let { seguroContext ->
                        Toast.makeText(seguroContext, "Error al cargar citas", Toast.LENGTH_SHORT).show()
                    }
                }
            }
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

    private fun abrirDetalleCita(cita: CitaPaciente) {
        val intent = Intent(requireContext(), DetalleCitaMedic::class.java)
        intent.putExtra("ID_CITA", cita.appointment.id)
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class CitasMedicAdapter(
        private val onCitaClick: (CitaPaciente) -> Unit
    ) : RecyclerView.Adapter<CitasMedicAdapter.ViewHolder>() {

        private var lastPosition = -1

        private val diffCallback = object : DiffUtil.ItemCallback<CitaPaciente>() {
            override fun areItemsTheSame(oldItem: CitaPaciente, newItem: CitaPaciente): Boolean {
                return oldItem.appointment.id == newItem.appointment.id
            }

            override fun areContentsTheSame(oldItem: CitaPaciente, newItem: CitaPaciente): Boolean {
                return oldItem == newItem
            }
        }

        private val differ = AsyncListDiffer(this, diffCallback)

        fun submitList(nuevaLista: List<CitaPaciente>) {
            lastPosition = -1
            differ.submitList(nuevaLista)
        }

        inner class ViewHolder(val itemBinding: MedicCuadroCitaBinding) : RecyclerView.ViewHolder(itemBinding.root) {
            fun bind(item: CitaPaciente) {
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

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val currentPosition = holder.bindingAdapterPosition
            if (currentPosition == RecyclerView.NO_POSITION) return

            val item = differ.currentList[currentPosition]
            holder.bind(item)

            holder.itemBinding.root.animate().cancel()

            holder.itemBinding.root.translationY = 0f
            holder.itemBinding.root.alpha = 1f

            if (currentPosition > lastPosition) {
                val view = holder.itemBinding.root
                view.translationY = 100f
                view.alpha = 0f

                view.animate()
                    .translationY(0f)
                    .alpha(1f)
                    .setStartDelay(currentPosition * 35L)
                    .setDuration(300L)
                    .setInterpolator(android.view.animation.DecelerateInterpolator())
                    .setListener(null)
                    .start()

                lastPosition = currentPosition
            }
        }

        override fun getItemCount() = differ.currentList.size

        override fun onViewDetachedFromWindow(holder: ViewHolder) {
            holder.itemBinding.root.clearAnimation()
            super.onViewDetachedFromWindow(holder)
        }
    }
}