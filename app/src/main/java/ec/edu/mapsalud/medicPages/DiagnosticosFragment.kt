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
import androidx.activity.viewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import ec.edu.mapsalud.dto.AppointmentDtoRemote
import ec.edu.mapsalud.dto.AppointmentPaciente
import ec.edu.mapsalud.dto.Paciente
import ec.edu.mapsalud.remote.impl.CitaRepositoryImpl
import ec.edu.mapsalud.remote.impl.UsuariosRepositoryImpl
import ec.edu.mapsalud.remote.inter.CitaRepository
import ec.edu.mapsalud.remote.inter.UsuariosRepository
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

    private lateinit var binding: MedicFragmentDiagnosticosBinding
    private lateinit var adapter: DiagnosticosAdapter
    private val auth = FirebaseAuth.getInstance()
    private val citaVM by viewModels<CitaViewModel>()
    private val usuarioVM by viewModels<UsuarioViewModel>()
    private val citaRepository = CitaRepositoryImpl()
    private val usuarioRepository = UsuariosRepositoryImpl()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = MedicFragmentDiagnosticosBinding.bind(view)

        configurarRecycler()
        configurarBuscador()
        initObservers()
        cargarTodasLasCitas()
    }

    private fun configurarRecycler() {
        adapter = DiagnosticosAdapter(emptyList()) { citaSeleccionada ->
            val intent = Intent(requireContext(), DetalleDiagnosticoMedic::class.java).apply {
                putExtra("APPOINTMENT_ID", citaSeleccionada.appointment.id)
                val nombreCompleto = "${citaSeleccionada.paciente.info.nombres} ${citaSeleccionada.paciente.info.apellidos}"
                putExtra("PACIENTE_NOMBRE", nombreCompleto)
                putExtra("MOTIVO_CITA", citaSeleccionada.appointment.reason)
            }
            startActivity(intent)
        }
        binding.recyclerCitasDiagnostico.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerCitasDiagnostico.adapter = adapter
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
                adapter.actualizarDatos(emptyList())
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

    private suspend fun cruzarCitasConPacientes(citasRemote: List<AppointmentDtoRemote>): List<AppointmentPaciente> {
        return coroutineScope {
            citasRemote.map { appointment ->
                async {
                    if (appointment.patientName.isNotEmpty()) {
                        val dummyPaciente = Paciente(info = ec.edu.mapsalud.dto.UsuarioInfo(
                            nombres = appointment.patientName,
                            apellidos = ""
                        ))
                        AppointmentPaciente(
                            appointment = appointment,
                            paciente = dummyPaciente,
                            horaFormateada = appointment.time,
                            amPm = ""
                        )
                    } else {
                        val patientResult = usuarioRepository.getPacienteById(appointment.idUser)
                        val paciente = patientResult.getOrNull() ?: Paciente()

                        AppointmentPaciente(
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
    private fun actualizarListaOrdenada(lista: List<AppointmentPaciente>) {
        val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        val listaOrdenada = lista.sortedWith(Comparator { c1, c2 ->
            val fecha1 = formato.parse(c1.appointment.date) ?: Date(0)
            val fecha2 = formato.parse(c2.appointment.date) ?: Date(0)
            fecha2.compareTo(fecha1)
        })

        adapter.actualizarDatos(listaOrdenada)
    }

    private fun obtenerFechaActualString(): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return sdf.format(Date())
    }

    inner class DiagnosticosAdapter(
        private var listaCitas: List<AppointmentPaciente>,
        private val onCitaClick: (AppointmentPaciente) -> Unit
    ) : RecyclerView.Adapter<DiagnosticosAdapter.ViewHolder>() {

        inner class ViewHolder(val binding: CuadroCitaCompletaBinding) : RecyclerView.ViewHolder(binding.root)

        fun actualizarDatos(nuevaLista: List<AppointmentPaciente>) {
            this.listaCitas = nuevaLista
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = CuadroCitaCompletaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val citaPaciente = listaCitas[position]
            val appointment = citaPaciente.appointment
            val infoUsuario = citaPaciente.paciente.info

            holder.binding.txtFechaCita.text = "${appointment.date} - ${appointment.time}"
            holder.binding.txtNombrePaciente.text = "${infoUsuario.nombres} ${infoUsuario.apellidos}"
            holder.binding.txtMotivo.text = appointment.reason

            holder.binding.colorBordeLateral.setBackgroundColor(Color.parseColor("#00838F"))
            holder.binding.root.setOnClickListener { onCitaClick(citaPaciente) }
        }

        override fun getItemCount() = listaCitas.size
    }
}