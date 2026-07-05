package ec.edu.mapsalud.patientPages

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ec.edu.mapsalud.R
import ec.edu.mapsalud.databinding.CuadroCitaCompletaBinding
import ec.edu.mapsalud.databinding.UserDiagnosticosBinding
import ec.edu.mapsalud.dto.CitaDetalle
import ec.edu.mapsalud.remote.impl.CitaRepositoryImpl
import com.google.firebase.auth.FirebaseAuth
import ec.edu.mapsalud.remote.impl.ConsultorioRepositoryImpl
import ec.edu.mapsalud.remote.impl.UsuariosRepositoryImpl
import ec.edu.mapsalud.usercases.citasUC.FetchAppointmentsWithDetailsUC
import ec.edu.mapsalud.viewmodel.CitaViewModel

class DiagnosticosFragment : Fragment(R.layout.user_diagnosticos) {

    private var _binding: UserDiagnosticosBinding? = null
    private val binding get() = _binding!!

    private lateinit var diagnosticosAdapter: DiagnosticosAdapter
    private val citaVM by viewModels<CitaViewModel>()
    private val citaRepository = CitaRepositoryImpl()
    private val consultorioRepository = ConsultorioRepositoryImpl()
    private val usuarioRepository = UsuariosRepositoryImpl()
    private val auth = FirebaseAuth.getInstance()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = UserDiagnosticosBinding.bind(view)

        configurarRecyclerView()
        initObservers()
        cargarDiagnosticos()
    }

    private fun configurarRecyclerView() {
        diagnosticosAdapter = DiagnosticosAdapter()
        binding.recyclerDiagnosticos.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerDiagnosticos.adapter = diagnosticosAdapter
    }
    private fun cargarDiagnosticos() {
        val userId = auth.currentUser?.uid ?: return
        citaVM.cargarCitasConDetalles(
            userId = userId,
            status = "Completada",
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
                binding.recyclerDiagnosticos.visibility = View.GONE
                diagnosticosAdapter.submitList(emptyList())
            } else {
                binding.txtEmptyState.visibility = View.GONE
                binding.recyclerDiagnosticos.visibility = View.VISIBLE
                diagnosticosAdapter.submitList(list)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class DiagnosticosAdapter : RecyclerView.Adapter<DiagnosticosAdapter.ViewHolder>() {

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
            val nombreDoctor = "Dr. ${item.doctor.info.nombres} ${item.doctor.info.apellidos}"

            holder.itemBinding.txtNombrePaciente.text = nombreDoctor
            holder.itemBinding.txtFechaCita.text = "${item.appointment.date} - ${item.appointment.time}"
            holder.itemBinding.txtMotivo.text = item.appointment.reason

            holder.itemBinding.root.setOnClickListener {
                val intent = Intent(requireContext(), DetalleDiagnosticoUser::class.java).apply {
                    putExtra("DOCTOR_NAME", nombreDoctor)
                    putExtra("FECHA", item.appointment.date)
                    // Controlamos los nulos enviando cadenas vacías o mensajes por defecto
                    putExtra("DIAGNOSTICO", item.appointment.diagnosis?.clinicalDiagnosis ?: "No especificado")
                    putExtra("TRATAMIENTO", item.appointment.diagnosis?.treatment ?: "No especificado")
                    putExtra("SUGERENCIAS", item.appointment.diagnosis?.suggestions ?: "Sin sugerencias adicionales")
                }
                startActivity(intent)
            }

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
