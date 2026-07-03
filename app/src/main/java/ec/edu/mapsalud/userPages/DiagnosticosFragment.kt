package ec.edu.mapsalud.userPages

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ec.edu.mapsalud.R
import ec.edu.mapsalud.databinding.CuadroCitaCompletaBinding
import ec.edu.mapsalud.databinding.UserDiagnosticosBinding
import ec.edu.mapsalud.dto.AppointmentDetail
import ec.edu.mapsalud.remote.impl.CitaRepositoryImpl
import ec.edu.mapsalud.remote.inter.CitaRepository
import com.google.firebase.auth.FirebaseAuth
import ec.edu.mapsalud.remote.impl.ConsultorioRepositoryImpl
import ec.edu.mapsalud.remote.impl.UsuariosRepositoryImpl
import ec.edu.mapsalud.usercases.citasUC.FetchAppointmentsWithDetailsUC
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ec.edu.mapsalud.viewmodel.CitaViewModel

class DiagnosticosFragment : Fragment(R.layout.user_diagnosticos) {

    private var _binding: UserDiagnosticosBinding? = null
    private val binding get() = _binding!!
    private val citaVM by viewModels<CitaViewModel>()
    private val citaRepository = CitaRepositoryImpl()
    private val consultorioRepository = ConsultorioRepositoryImpl()
    private val usuarioRepository = UsuariosRepositoryImpl()
    private val auth = FirebaseAuth.getInstance()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = UserDiagnosticosBinding.bind(view)

        binding.recyclerDiagnosticos.layoutManager = LinearLayoutManager(requireContext())
        initObservers()
        cargarDiagnosticos()
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
            if (list.isEmpty()) {
                binding.txtEmptyState.visibility = View.VISIBLE
                binding.recyclerDiagnosticos.visibility = View.GONE
            } else {
                binding.txtEmptyState.visibility = View.GONE
                binding.recyclerDiagnosticos.visibility = View.VISIBLE
                binding.recyclerDiagnosticos.adapter = DiagnosticosAdapter(list)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class DiagnosticosAdapter(private val items: List<AppointmentDetail>) :
        RecyclerView.Adapter<DiagnosticosAdapter.ViewHolder>() {

        inner class ViewHolder(val itemBinding: CuadroCitaCompletaBinding) :
            RecyclerView.ViewHolder(itemBinding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = CuadroCitaCompletaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            val nombreDoctor = "Dr. ${item.doctor.info.nombres} ${item.doctor.info.apellidos}"
            holder.itemBinding.txtNombrePaciente.text = nombreDoctor
            holder.itemBinding.txtFechaCita.text = "${item.appointment.date} - ${item.appointment.time}"
            holder.itemBinding.txtMotivo.text = item.appointment.reason

            holder.itemBinding.root.setOnClickListener {
                val intent = Intent(requireContext(), DetalleDiagnosticoUser::class.java).apply {
                    putExtra("DOCTOR_NAME", nombreDoctor)
                    putExtra("FECHA", item.appointment.date)
                    putExtra("DIAGNOSTICO", item.appointment.diagnosis?.clinicalDiagnosis)
                    putExtra("TRATAMIENTO", item.appointment.diagnosis?.treatment)
                    putExtra("SUGERENCIAS", item.appointment.diagnosis?.suggestions)
                }
                startActivity(intent)
            }
        }

        override fun getItemCount() = items.size
    }
}
