package ec.edu.mapsalud.userPages

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import ec.edu.mapsalud.dto.CitaMedica
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import ec.edu.mapsalud.databinding.UserCancelarCitaBinding
import ec.edu.mapsalud.databinding.UserCuadroCitaBinding


class CancelarCita : AppCompatActivity() {

    private lateinit var binding: UserCancelarCitaBinding
    private var posicionSeleccionada = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = UserCancelarCitaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val listaDemo = listOf(
            CitaMedica("Dr. Andrés Gómez", "15 Oct, 2026 • 09:30 AM"),
            CitaMedica("Dra. Elena Rivas", "22 Oct, 2026 • 14:00 PM")
        )

        val adapter = CitaCancelableAdapter(listaDemo) { posicion ->
            posicionSeleccionada = posicion
            binding.btnCancelarCita.isEnabled = true
        }

        binding.recyclerCitas.layoutManager = LinearLayoutManager(this)
        binding.recyclerCitas.adapter = adapter

        binding.btnCancelarCita.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Cancelar Cita")
                .setMessage("¿Seguro de que quiero cancelar la cita?")
                .setPositiveButton("Sí, cancelar") { dialog, _ ->
                    dialog.dismiss()
                    regresarConExito()
                }
                .setNegativeButton("No", null)
                .show()
        }

        binding.btnRegresar.setOnClickListener {
            finish()
        }
    }

    private fun regresarConExito() {
        val intent = Intent(this, PrincipalUser::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        intent.putExtra("MENSAJE_SNACKBAR", "Cita cancelada")
        startActivity(intent)
        finish()
    }

    inner class CitaCancelableAdapter(
        private val citas: List<CitaMedica>,
        private val onCitaSeleccionada: (Int) -> Unit
    ) : RecyclerView.Adapter<CitaCancelableAdapter.ViewHolder>() {

        inner class ViewHolder(val itemBinding: UserCuadroCitaBinding) : RecyclerView.ViewHolder(itemBinding.root) {
            fun bind(cita: CitaMedica, position: Int) {
                itemBinding.txtDoctorCancel.text = cita.doctor
                itemBinding.txtFechaCancel.text = cita.fechaHora

                itemBinding.radioSeleccion.isChecked = position == posicionSeleccionada

                itemBinding.root.setOnClickListener {
                    val posicionAnterior = posicionSeleccionada
                    posicionSeleccionada = adapterPosition

                    notifyItemChanged(posicionAnterior)
                    notifyItemChanged(posicionSeleccionada)

                    onCitaSeleccionada(posicionSeleccionada)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
            UserCuadroCitaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
        override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(citas[position], position)
        override fun getItemCount() = citas.size
    }
}