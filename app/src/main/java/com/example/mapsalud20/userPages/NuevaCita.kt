package com.example.mapsalud20.userPages

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mapsalud20.databinding.UserNuevaCitaBinding
import com.example.mapsalud20.databinding.CuadroCentroMedicoBinding
import com.example.mapsalud20.dto.CentroMedico

class NuevaCita : AppCompatActivity() {

    private lateinit var binding: UserNuevaCitaBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = UserNuevaCitaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnRegresar.setOnClickListener {
            finish()
        }

        configurarFiltros()
        configurarRecyclerView()
    }

    private fun configurarFiltros() {

        val tiposCentro = arrayOf("Public centers", "Private centers", "All centers")
        val adapterTipos = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, tiposCentro)
        binding.autoCompleteTipoCentro.setAdapter(adapterTipos)

        val especialidades = arrayOf("Cardiology", "Neurology", "Pediatrics", "Traumatology")
        val adapterEspecialidades = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, especialidades)
        binding.autoCompleteEspecialidad.setAdapter(adapterEspecialidades)
    }

    private fun configurarRecyclerView() {

        val listaDemo = listOf(
            CentroMedico("St. Jude Medical", "Cardiology Specialists", "PUBLIC", "45m away"),
            CentroMedico("Apex Cardiology", "Advanced Heart Institute", "PRIVATE", "82m away"),
            CentroMedico("City Public Health", "Multidisciplinary Center", "PUBLIC", "98m away")
        )

        val adapter = CentroMedicoAdapter(listaDemo) { centroSeleccionado ->

            val intent = Intent(this, Especialistas::class.java)
            intent.putExtra("CENTRO_NOMBRE", centroSeleccionado.nombre)
            startActivity(intent)
        }

        binding.recyclerViewCentros.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewCentros.adapter = adapter
    }

    inner class CentroMedicoAdapter(
        private val listaCentros: List<CentroMedico>,
        private val onClick: (CentroMedico) -> Unit
    ) : RecyclerView.Adapter<CentroMedicoAdapter.CentroViewHolder>() {

        inner class CentroViewHolder(val binding: CuadroCentroMedicoBinding) : RecyclerView.ViewHolder(binding.root) {
            fun bind(centro: CentroMedico) {
                binding.txtNombreCentro.text = centro.nombre
                binding.txtEspecialidad.text = centro.especialidad
                binding.txtTipoCentro.text = centro.tipo
                binding.txtDistancia.text = centro.distancia

                binding.root.setOnClickListener {
                    onClick(centro)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CentroViewHolder {
            val binding = CuadroCentroMedicoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return CentroViewHolder(binding)
        }

        override fun onBindViewHolder(holder: CentroViewHolder, position: Int) {
            holder.bind(listaCentros[position])
        }

        override fun getItemCount() = listaCentros.size
    }
}