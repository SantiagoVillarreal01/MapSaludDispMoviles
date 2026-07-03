package ec.edu.mapsalud.userPages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ec.edu.mapsalud.R
import ec.edu.mapsalud.databinding.CuadroCitaCompletaBinding
import ec.edu.mapsalud.databinding.UserCancelarCitaBinding
import ec.edu.mapsalud.dto.AppointmentDetail
import ec.edu.mapsalud.remote.impl.CitaRepositoryImpl
import ec.edu.mapsalud.remote.inter.CitaRepository
import com.google.firebase.auth.FirebaseAuth
import ec.edu.mapsalud.remote.impl.ConsultorioRepositoryImpl
import ec.edu.mapsalud.remote.impl.UsuariosRepositoryImpl
import ec.edu.mapsalud.usercases.citasUC.CancelAppointmentUC
import ec.edu.mapsalud.usercases.citasUC.FetchAppointmentsWithDetailsUC
import ec.edu.mapsalud.viewmodel.CitaViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CancelarCitaFragment : Fragment(R.layout.user_cancelar_cita) {

    private var _binding: UserCancelarCitaBinding? = null
    private val binding get() = _binding!!
    private val citaVM by viewModels<CitaViewModel>()
    private val citaRepository = CitaRepositoryImpl()
    private val consultorioRepository = ConsultorioRepositoryImpl()
    private val usuarioRepository = UsuariosRepositoryImpl()
    private val auth = FirebaseAuth.getInstance()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = UserCancelarCitaBinding.bind(view)

        binding.recyclerCitas.layoutManager = LinearLayoutManager(requireContext())
        initObservers()
        cargarCitasPendientes()
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
            if (list.isEmpty()) {
                binding.txtEmptyState.visibility = View.VISIBLE
                binding.recyclerCitas.visibility = View.GONE
            } else {
                binding.txtEmptyState.visibility = View.GONE
                binding.recyclerCitas.visibility = View.VISIBLE
                binding.recyclerCitas.adapter = CancelarAdapter(list) { appointment ->
                    confirmarCancelacion(appointment)
                }
            }
        }

        citaVM.operationSuccess.observe(viewLifecycleOwner) { exito ->
            if (exito) {
                Toast.makeText(requireContext(), "Cita cancelada correctamente", Toast.LENGTH_SHORT).show()
                cargarCitasPendientes()
            } else {
                Toast.makeText(requireContext(), "Error al cancelar la cita", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun confirmarCancelacion(appointment: AppointmentDetail) {
        AlertDialog.Builder(requireContext())
            .setTitle("Cancelar Cita")
            .setMessage("¿Estás seguro de que deseas cancelar tu cita con el Dr. ${appointment.doctor.info.nombres}?")
            .setPositiveButton("Sí, cancelar") { _, _ ->
                ejecutarCancelacion(appointment.appointment.id)
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun ejecutarCancelacion(appointmentId: String) {
        citaVM.cancelarCita(
            appointmentId = appointmentId,
            cancelUC = CancelAppointmentUC(citaRepository)
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class CancelarAdapter(
        private val items: List<AppointmentDetail>,
        private val onCancelClick: (AppointmentDetail) -> Unit
    ) : RecyclerView.Adapter<CancelarAdapter.ViewHolder>() {

        inner class ViewHolder(val itemBinding: CuadroCitaCompletaBinding) :
            RecyclerView.ViewHolder(itemBinding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = CuadroCitaCompletaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.itemBinding.txtNombrePaciente.text = "Dr. ${item.doctor.info.nombres} ${item.doctor.info.apellidos}"
            holder.itemBinding.txtFechaCita.text = "${item.appointment.date} - ${item.appointment.time}"
            holder.itemBinding.txtMotivo.text = item.appointment.reason
            holder.itemBinding.root.setOnClickListener { onCancelClick(item) }
        }

        override fun getItemCount() = items.size
    }
}
