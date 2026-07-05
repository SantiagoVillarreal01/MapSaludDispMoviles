package ec.edu.mapsalud.medicPages

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ec.edu.mapsalud.R
import ec.edu.mapsalud.databinding.CuadroCitaCompletaBinding
import ec.edu.mapsalud.databinding.MedicFragmentDiagnosticosBinding
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import com.google.firebase.auth.FirebaseAuth
import ec.edu.mapsalud.dto.CitaDtoRemote
import ec.edu.mapsalud.dto.CitaPaciente
import ec.edu.mapsalud.dto.Paciente
import ec.edu.mapsalud.remote.impl.CitaRepositoryImpl
import ec.edu.mapsalud.remote.impl.UsuariosRepositoryImpl
import ec.edu.mapsalud.usercases.citasUC.GetCompletedAppointmentsByDoctorAndPatientUC
import ec.edu.mapsalud.usercases.citasUC.GetCompletedAppointmentsByDoctorUC
import ec.edu.mapsalud.usercases.usuariosUC.GetPacienteByCedulaUC
import ec.edu.mapsalud.viewmodel.CitaViewModel
import ec.edu.mapsalud.viewmodel.UsuarioViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.getValue

class DiagnosticosFragment : Fragment(R.layout.medic_fragment_diagnosticos) {

    private var _binding: MedicFragmentDiagnosticosBinding? = null
    private val binding get() = _binding!!
    private lateinit var diagnosticosAdapter: DiagnosticosAdapter
    private val auth = FirebaseAuth.getInstance()
    private val citaVM by viewModels<CitaViewModel>()
    private val usuarioVM by viewModels<UsuarioViewModel>()
    private val citaRepository = CitaRepositoryImpl()
    private val usuarioRepository = UsuariosRepositoryImpl()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = MedicFragmentDiagnosticosBinding.bind(view)

        configurarRecycler()
        configurarBuscador()
        initObservers()
        cargarTodasLasCitas()
    }

    private fun configurarRecycler() {
        diagnosticosAdapter = DiagnosticosAdapter { citaSeleccionada ->
            val intent = Intent(requireContext(), DetalleDiagnosticoMedic::class.java).apply {
                putExtra("APPOINTMENT_ID", citaSeleccionada.appointment.id)
                val nombreCompleto = "${citaSeleccionada.paciente.info.nombres} ${citaSeleccionada.paciente.info.apellidos}"
                putExtra("PACIENTE_NOMBRE", nombreCompleto)
                putExtra("MOTIVO_CITA", citaSeleccionada.appointment.reason)
            }
            startActivity(intent)
        }
        binding.recyclerCitasDiagnostico.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerCitasDiagnostico.adapter = diagnosticosAdapter
    }

    private fun configurarBuscador() {
        binding.btnBuscar.setOnClickListener {
            val cedula = binding.inputBuscarCedula.text.toString().trim()
            if (cedula.isEmpty()) {
                cargarTodasLasCitas()
            } else {
                buscarCitasPorCedula(cedula)
            }
        }
    }
    private fun cargarTodasLasCitas() {
        val uidMedico = auth.currentUser?.uid ?: return

        citaVM.cargarCitasCompletadasPorDoctor(
            uidMedico,
            GetCompletedAppointmentsByDoctorUC(citaRepository)
        )
    }

    private fun buscarCitasPorCedula(cedula: String) {
        usuarioVM.buscarPacientePorCedula(
            cedula = cedula,
            getPacienteByCedulaUC = GetPacienteByCedulaUC(usuarioRepository)
        )
    }

    private fun initObservers() {
        citaVM.appointmentsRawList.observe(viewLifecycleOwner) { citasRemote ->
            lifecycleScope.launch(Dispatchers.Main) {
                try {
                    val listaCompleta = withContext(Dispatchers.IO) {
                        cruzarCitasConPacientes(citasRemote)
                    }
                    actualizarListaOrdenada(listaCompleta)
                } catch (e: Exception) {
                    Log.e("DiagnosticosFragment", "Error al cargar histórico: ${e.message}", e)
                    Toast.makeText(requireContext(), "Error al cargar el historial de citas", Toast.LENGTH_SHORT).show()
                }
            }
        }
        usuarioVM.paciente.observe(viewLifecycleOwner) { paciente ->
            val uidMedico = auth.currentUser?.uid ?: return@observe

            if (paciente == null) {
                Toast.makeText(requireContext(), "No se encontró paciente con esa cédula", Toast.LENGTH_SHORT).show()
                diagnosticosAdapter.submitList(emptyList())
                return@observe
            }
            citaVM.cargarHistorialCompartido(
                idDoctor = uidMedico,
                idUser = paciente.info.id,
                getByDoctorAndPatientUC = GetCompletedAppointmentsByDoctorAndPatientUC(
                    citaRepository
                )
            )
        }
    }

    private suspend fun cruzarCitasConPacientes(citasRemote: List<CitaDtoRemote>): List<CitaPaciente> {
        return coroutineScope {
            citasRemote.map { appointment ->
                async {
                    if (appointment.patientName.isNotEmpty()) {
                        val dummyPaciente = Paciente(info = ec.edu.mapsalud.dto.UsuarioInfo(
                            nombres = appointment.patientName,
                            apellidos = ""
                        ))
                        CitaPaciente(
                            appointment = appointment,
                            paciente = dummyPaciente,
                            horaFormateada = appointment.time,
                            amPm = ""
                        )
                    } else {
                        val patientResult = usuarioRepository.getPacienteById(appointment.idUser)
                        val paciente = patientResult.getOrNull() ?: Paciente()

                        CitaPaciente(
                            appointment = appointment,
                            paciente = paciente,
                            horaFormateada = appointment.time,
                            amPm = ""
                        )
                    }
                }
            }.awaitAll()
        }
    }
    private fun actualizarListaOrdenada(lista: List<CitaPaciente>) {
        val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        val listaOrdenada = lista.sortedWith(Comparator { c1, c2 ->
            val fecha1 = formato.parse(c1.appointment.date) ?: Date(0)
            val fecha2 = formato.parse(c2.appointment.date) ?: Date(0)
            fecha2.compareTo(fecha1)
        })

        diagnosticosAdapter.submitList(listaOrdenada)
    }

    inner class DiagnosticosAdapter(
        private val onCitaClick: (CitaPaciente) -> Unit
    ) : RecyclerView.Adapter<DiagnosticosAdapter.ViewHolder>() {

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

        inner class ViewHolder(val itemBinding: CuadroCitaCompletaBinding) : RecyclerView.ViewHolder(itemBinding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = CuadroCitaCompletaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val currentPosition = holder.bindingAdapterPosition
            if (currentPosition == RecyclerView.NO_POSITION) return

            val citaPaciente = differ.currentList[currentPosition]
            val appointment = citaPaciente.appointment
            val infoUsuario = citaPaciente.paciente.info

            holder.itemBinding.txtFechaCita.text = "${appointment.date} - ${appointment.time}"
            holder.itemBinding.txtNombrePaciente.text = "${infoUsuario.nombres} ${infoUsuario.apellidos}"
            holder.itemBinding.txtMotivo.text = appointment.reason

            holder.itemBinding.colorBordeLateral.setBackgroundColor(Color.parseColor("#00838F"))
            holder.itemBinding.root.setOnClickListener { onCitaClick(citaPaciente) }

            holder.itemBinding.root.animate().setListener(null).cancel()
            holder.itemBinding.root.translationY = 0f
            holder.itemBinding.root.alpha = 1f
            holder.itemBinding.root.scaleX = 1f
            holder.itemBinding.root.scaleY = 1f

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