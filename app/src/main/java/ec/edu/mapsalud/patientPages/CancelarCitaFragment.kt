package ec.edu.mapsalud.patientPages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ec.edu.mapsalud.R
import ec.edu.mapsalud.databinding.CuadroCitaCompletaBinding
import ec.edu.mapsalud.databinding.UserCancelarCitaBinding
import ec.edu.mapsalud.dto.CitaDetalle
import ec.edu.mapsalud.remote.impl.CitaRepositoryImpl
import com.google.firebase.auth.FirebaseAuth
import ec.edu.mapsalud.remote.impl.ConsultorioRepositoryImpl
import ec.edu.mapsalud.remote.impl.UsuariosRepositoryImpl
import ec.edu.mapsalud.usercases.citasUC.CancelAppointmentUC
import ec.edu.mapsalud.usercases.citasUC.FetchAppointmentsWithDetailsUC
import ec.edu.mapsalud.viewmodel.CitaViewModel

class CancelarCitaFragment : Fragment(R.layout.user_cancelar_cita) {

    private var _binding: UserCancelarCitaBinding? = null
    private val binding get() = _binding!!
    private lateinit var cancelarAdapter: CancelarAdapter
    private val citaVM by viewModels<CitaViewModel>()
    private val citaRepository = CitaRepositoryImpl()
    private val consultorioRepository = ConsultorioRepositoryImpl()
    private val usuarioRepository = UsuariosRepositoryImpl()
    private val auth = FirebaseAuth.getInstance()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = UserCancelarCitaBinding.bind(view)

        configurarRecyclerView()
        initObservers()
        cargarCitasPendientes()
    }

    private fun configurarRecyclerView() {
        cancelarAdapter = CancelarAdapter { appointment ->
            confirmarCancelacion(appointment)
        }
        binding.recyclerCitas.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerCitas.adapter = cancelarAdapter
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
            if (list.isNullOrEmpty()) {
                binding.txtEmptyState.visibility = View.VISIBLE
                binding.recyclerCitas.visibility = View.GONE
                cancelarAdapter.submitList(emptyList())
            } else {
                binding.txtEmptyState.visibility = View.GONE
                binding.recyclerCitas.visibility = View.VISIBLE
                cancelarAdapter.submitList(list)
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

    private fun confirmarCancelacion(appointment: CitaDetalle) {
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
        private val onCancelClick: (CitaDetalle) -> Unit
    ) : RecyclerView.Adapter<CancelarAdapter.ViewHolder>() {
        private var lastPosition = -1
        private val diffCallback = object : DiffUtil.ItemCallback<CitaDetalle>() {
            override fun areItemsTheSame(oldItem: CitaDetalle, newItem: CitaDetalle): Boolean {
                return oldItem.appointment.id == newItem.appointment.id
            }

            override fun areContentsTheSame(oldItem: CitaDetalle, newItem: CitaDetalle): Boolean {
                return oldItem == newItem
            }
        }

        private val differ = AsyncListDiffer(this, diffCallback)

        fun submitList(nuevaLista: List<CitaDetalle>) {
            lastPosition = -1
            differ.submitList(nuevaLista)
        }

        inner class ViewHolder(val itemBinding: CuadroCitaCompletaBinding) :
            RecyclerView.ViewHolder(itemBinding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = CuadroCitaCompletaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val currentPosition = holder.bindingAdapterPosition
            if (currentPosition == RecyclerView.NO_POSITION) return

            val item = differ.currentList[currentPosition]

            holder.itemBinding.txtNombrePaciente.text = "Dr. ${item.doctor.info.nombres} ${item.doctor.info.apellidos}"
            holder.itemBinding.txtFechaCita.text = "${item.appointment.date} - ${item.appointment.time}"
            holder.itemBinding.txtMotivo.text = item.appointment.reason
            holder.itemBinding.root.setOnClickListener { onCancelClick(item) }

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
