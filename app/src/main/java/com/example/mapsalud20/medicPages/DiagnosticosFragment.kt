package com.example.mapsalud20.medicPages

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mapsalud20.R
import com.example.mapsalud20.databinding.MedicFragmentDiagnosticosBinding
import com.example.mapsalud20.databinding.CuadroCitaCompletaBinding
import com.example.mapsalud20.dto.CitaParaDiagnostico

class DiagnosticosFragment : Fragment(R.layout.medic_fragment_diagnosticos) {

    private lateinit var binding: MedicFragmentDiagnosticosBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = MedicFragmentDiagnosticosBinding.bind(view)

        configurarRecycler()
    }

    private fun configurarRecycler() {
        //Datos simulados
        val citas = listOf(
            CitaParaDiagnostico("1", "15 MAY 2024", "Juan Delgado", "Consulta General", "#00838F"),
            CitaParaDiagnostico("2", "02 ABR 2024", "María López", "Cardiología", "#D32F2F")
        )

        binding.recyclerCitasDiagnostico.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerCitasDiagnostico.adapter = DiagnosticosAdapter(citas) { citaSeleccionada ->
            //Abrir la nueva Activity de enviar diagnóstico
            val intent = Intent(requireContext(), DetalleDiagnosticoMedic::class.java)
            intent.putExtra("PACIENTE_NOMBRE", citaSeleccionada.paciente)
            intent.putExtra("MOTIVO_CITA", citaSeleccionada.motivo)
            startActivity(intent)
        }
    }

    inner class DiagnosticosAdapter(
        private val listaCitas: List<CitaParaDiagnostico>,
        private val onCitaClick: (CitaParaDiagnostico) -> Unit
    ) : RecyclerView.Adapter<DiagnosticosAdapter.ViewHolder>() {

        inner class ViewHolder(val binding: CuadroCitaCompletaBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = CuadroCitaCompletaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val cita = listaCitas[position]
            holder.binding.txtFechaCita.text = cita.fecha
            holder.binding.txtNombrePaciente.text = cita.paciente
            holder.binding.txtMotivo.text = cita.motivo
            holder.binding.colorBordeLateral.setBackgroundColor(android.graphics.Color.parseColor(cita.colorBorde))

            holder.binding.root.setOnClickListener { onCitaClick(cita) }
        }

        override fun getItemCount() = listaCitas.size
    }
}