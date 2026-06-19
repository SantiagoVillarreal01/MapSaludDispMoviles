package ec.edu.mapsalud.userPages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ec.edu.mapsalud.databinding.CuadroDiagnosticoBinding
import ec.edu.mapsalud.databinding.UserDiagnosticosBinding
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import ec.edu.mapsalud.dto.AppointmentDetail
import ec.edu.mapsalud.remote.impl.AppointmentRemoteImpl
import android.view.View

class Diagnosticos : AppCompatActivity() {

    private lateinit var binding: UserDiagnosticosBinding
    private val appointmentRemote = AppointmentRemoteImpl()

    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: "ID_TEMP"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = UserDiagnosticosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.recyclerDiagnosticos.layoutManager = LinearLayoutManager(this)

        binding.btnRegresar.setOnClickListener {
            finish()
        }

        cargarDiagnosticos()
    }

    private fun cargarDiagnosticos() {
        lifecycleScope.launch {
            val result = appointmentRemote.getCompletedAppointmentsWithDiagnosis(currentUserId)

            result.onSuccess { listaDetallada ->
                if (listaDetallada.isEmpty()) {
                    binding.recyclerDiagnosticos.visibility = View.GONE
                    binding.txtEmptyState.visibility = View.VISIBLE
                } else {
                    binding.recyclerDiagnosticos.visibility = View.VISIBLE
                    binding.txtEmptyState.visibility = View.GONE
                    binding.recyclerDiagnosticos.adapter = DiagnosticoAdapter(listaDetallada)
                }
            }.onFailure {
                Toast.makeText(this@Diagnosticos, "Error al cargar los diagnósticos.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    inner class DiagnosticoAdapter(private val listaCitas: List<AppointmentDetail>) :
        RecyclerView.Adapter<DiagnosticoAdapter.ViewHolder>() {

        inner class ViewHolder(val itemBinding: CuadroDiagnosticoBinding) : RecyclerView.ViewHolder(itemBinding.root) {

            fun bind(detalle: AppointmentDetail) {
                val cita = detalle.appointment
                val medico = detalle.doctor
                val diagnosticoInfo = cita.diagnosis

                itemBinding.txtMotivoCita.text = cita.reason

                val fechaEnvio = diagnosticoInfo?.dateGiven ?: cita.date

                itemBinding.txtDoctorYFecha.text = "Dr. ${medico.info.nombres} ${medico.info.apellidos} • $fechaEnvio"

                itemBinding.txtDiagnosticoClinico.text = diagnosticoInfo?.clinicalDiagnosis ?: "No especificado"
                itemBinding.txtTratamiento.text = diagnosticoInfo?.treatment ?: "Ninguno"
                itemBinding.txtSugerencias.text = diagnosticoInfo?.suggestions ?: "Ninguna"
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = CuadroDiagnosticoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(listaCitas[position])
        }

        override fun getItemCount() = listaCitas.size
    }
}