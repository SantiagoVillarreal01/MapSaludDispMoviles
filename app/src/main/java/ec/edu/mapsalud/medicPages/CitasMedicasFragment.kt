package com.example.mapsalud.medicPages

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.example.mapsalud.dto.CitaMedic
import ec.edu.mapsalud.R
import ec.edu.mapsalud.databinding.MedicCuadroCitaBinding
import ec.edu.mapsalud.databinding.MedicFragmentCitasBinding

class CitasMedicasFragment : Fragment(R.layout.medic_fragment_citas) {

    private lateinit var binding: MedicFragmentCitasBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = MedicFragmentCitasBinding.bind(view)

        val citasHoy = listOf(
            CitaMedic("09:00", "AM", "Ana María González", "Control Post-operatorio", "En espera", "#00695C", "#B2EBF2", "#006064"),
            CitaMedic("10:30", "AM", "Carlos Javier Ruiz", "Chequeo General Anual", "Pendiente", "#9E9E9E", "#E0E0E0", "#616161"),
            CitaMedic("11:15", "AM", "Elena Rodriguez", "Urgencia - Dolor Abdominal", "Urgente", "#D32F2F", "#FFCDD2", "#C62828"),
            CitaMedic("12:00", "PM", "Roberto Valdez", "Consulta Virtual", "Confirmado", "#E0E0E0", "#EEEEEE", "#9E9E9E")
        )

        val citasManana = listOf(
            CitaMedic("08:00", "AM", "Lucía Mendoza", "Control de Presión", "Pendiente", "#9E9E9E", "#E0E0E0", "#616161"),
            CitaMedic("09:30", "AM", "Fernando Silva", "Lectura de Exámenes", "Pendiente", "#9E9E9E", "#E0E0E0", "#616161")
        )

        binding.recyclerCitasHoy.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerCitasHoy.adapter = CitasMedicAdapter(citasHoy) { cita ->
            abrirDetalleCita(cita)
        }

        binding.recyclerCitasManana.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerCitasManana.adapter = CitasMedicAdapter(citasManana) { cita ->
            abrirDetalleCita(cita)
        }
    }

    private fun abrirDetalleCita(cita: CitaMedic) {
        val intent = Intent(requireContext(), DetalleCitaMedic::class.java)
        intent.putExtra("NOMBRE_PACIENTE", cita.nombrePaciente)
        startActivity(intent)
    }

    inner class CitasMedicAdapter(
        private val citas: List<CitaMedic>,
        private val onCitaClick: (CitaMedic) -> Unit
    ) : RecyclerView.Adapter<CitasMedicAdapter.ViewHolder>() {

        inner class ViewHolder(val itemBinding: MedicCuadroCitaBinding) : RecyclerView.ViewHolder(itemBinding.root) {
            fun bind(cita: CitaMedic) {
                itemBinding.txtHoraCita.text = cita.hora
                itemBinding.txtAmPmCita.text = cita.amPm
                itemBinding.txtNombrePaciente.text = cita.nombrePaciente
                itemBinding.txtMotivoCita.text = cita.motivo
                itemBinding.txtEstadoCita.text = cita.estado

                itemBinding.indicadorColor.setBackgroundColor(Color.parseColor(cita.colorBorde))
                itemBinding.cardEstado.setCardBackgroundColor(Color.parseColor(cita.colorFondoEstado))
                itemBinding.txtEstadoCita.setTextColor(Color.parseColor(cita.colorTextoEstado))

                itemBinding.root.setOnClickListener {
                    onCitaClick(cita)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(MedicCuadroCitaBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(citas[position])
        }

        override fun getItemCount() = citas.size
    }
}