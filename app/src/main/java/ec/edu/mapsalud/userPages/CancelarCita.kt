package ec.edu.mapsalud.userPages

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import ec.edu.mapsalud.databinding.UserCancelarCitaBinding
import android.graphics.Color
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import ec.edu.mapsalud.databinding.CuadroCitaPendienteBinding
import ec.edu.mapsalud.dto.AppointmentDetail
import ec.edu.mapsalud.remote.impl.AppointmentRemoteImpl
import kotlinx.coroutines.launch
import android.view.View

class CancelarCita : AppCompatActivity() {

    private lateinit var binding: UserCancelarCitaBinding
    private val appointmentRemote = AppointmentRemoteImpl()

    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: "ID_TEMP"
    private var citaSeleccionada: AppointmentDetail? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = UserCancelarCitaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.recyclerCitas.layoutManager = LinearLayoutManager(this)

        binding.btnRegresar.setOnClickListener { finish() }

        binding.btnCancelarCita.setOnClickListener {
            mostrarDialogoCancelacion()
        }

        cargarCitasPendientes()
    }

    private fun cargarCitasPendientes() {
        lifecycleScope.launch {
            val result = appointmentRemote.getPendingAppointmentsWithDetails(currentUserId)

            result.onSuccess { citas ->
                if (citas.isEmpty()) {
                    binding.recyclerCitas.visibility = View.GONE
                    binding.txtEmptyState.visibility = View.VISIBLE
                } else {
                    binding.recyclerCitas.visibility = View.VISIBLE
                    binding.txtEmptyState.visibility = View.GONE
                    binding.recyclerCitas.adapter = CitaCancelableAdapter(citas)
                }
            }.onFailure {
                Toast.makeText(this@CancelarCita, "Error al cargar las citas.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun mostrarDialogoCancelacion() {
        val apellidoMedico = citaSeleccionada?.doctor?.info?.apellidos ?: ""

        MaterialAlertDialogBuilder(this)
            .setTitle("Cancelar Cita")
            .setMessage("¿Estás seguro de que deseas cancelar la cita con el Dr. $apellidoMedico? Esta acción no se puede deshacer.")
            .setPositiveButton("Sí, cancelar") { dialog, _ ->
                dialog.dismiss()
                procesarCancelacion()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun procesarCancelacion() {
        val idCita = citaSeleccionada?.appointment?.id ?: return

        binding.btnCancelarCita.isEnabled = false
        binding.btnCancelarCita.text = "Cancelando..."

        lifecycleScope.launch {
            appointmentRemote.cancelAppointment(idCita)
                .onSuccess {
                    regresarConExito()
                }
                .onFailure {
                    Toast.makeText(this@CancelarCita, "Error al cancelar la cita", Toast.LENGTH_SHORT).show()
                    binding.btnCancelarCita.isEnabled = true
                    binding.btnCancelarCita.text = "Cancelar Cita"
                }
        }
    }

    private fun regresarConExito() {
        val intent = Intent(this, PrincipalUser::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        intent.putExtra("MENSAJE_SNACKBAR", "Cita cancelada con éxito")
        startActivity(intent)
        finish()
    }

    inner class CitaCancelableAdapter(
        private val listaCitas: List<AppointmentDetail>
    ) : RecyclerView.Adapter<CitaCancelableAdapter.ViewHolder>() {

        private var posicionSeleccionada = -1

        inner class ViewHolder(val itemBinding: CuadroCitaPendienteBinding) : RecyclerView.ViewHolder(itemBinding.root) {

            fun bind(detalle: AppointmentDetail, posicion: Int) {
                // CORRECCIÓN: Acceso a doctor.info
                val primerNombre = detalle.doctor.info.nombres.split(" ").firstOrNull() ?: ""
                val primerApellido = detalle.doctor.info.apellidos.split(" ").firstOrNull() ?: ""

                itemBinding.txtNombreMedico.text = "Dr. $primerNombre $primerApellido"
                itemBinding.txtEspecialidad.text = detalle.office.specialty
                itemBinding.txtFechaHora.text = "📅 ${detalle.appointment.date} • 🕒 ${detalle.appointment.time}"

                val isSelected = (posicion == posicionSeleccionada)

                if (isSelected) {
                    itemBinding.root.strokeColor = Color.parseColor("#D32F2F")
                    itemBinding.root.strokeWidth = 6
                    itemBinding.root.setCardBackgroundColor(Color.parseColor("#FFEBEE"))
                } else {
                    itemBinding.root.strokeColor = Color.parseColor("#EAEAEA")
                    itemBinding.root.strokeWidth = 2
                    itemBinding.root.setCardBackgroundColor(Color.WHITE)
                }

                itemBinding.root.setOnClickListener {
                    val posicionAnterior = posicionSeleccionada
                    posicionSeleccionada = bindingAdapterPosition

                    citaSeleccionada = detalle
                    binding.btnCancelarCita.isEnabled = true
                    notifyItemChanged(posicionAnterior)
                    notifyItemChanged(posicionSeleccionada)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val itemBinding = CuadroCitaPendienteBinding.inflate(LayoutInflater.from(parent.context), parent, false)

            val params = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, 0, 24)
            itemBinding.root.layoutParams = params

            return ViewHolder(itemBinding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(listaCitas[position], position)
        }

        override fun getItemCount() = listaCitas.size
    }
}