package com.example.mapsalud20.userPages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mapsalud20.databinding.UserDiagnosticosBinding
import com.example.mapsalud20.databinding.CuadroDiagnosticoBinding
import com.example.mapsalud20.dto.Diagnostico
import com.example.mapsalud20.dto.Medicamento


class Diagnosticos : AppCompatActivity() {

    private lateinit var binding: UserDiagnosticosBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = UserDiagnosticosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val listaDemo = listOf(
            Diagnostico(
                "Chequeo General", "Dra. Elena Rivas • 05 Oct, 2026",
                "Todo en orden. Mantener dieta baja en sodio y hacer 30 min de ejercicio diario.",
                listOf(Medicamento("Vitamina C", "1 pastilla diaria al desayunar"))
            ),
            Diagnostico(
                "Infección Respiratoria", "Dr. Andrés Gómez • 20 Sep, 2026",
                "Reposo absoluto por 3 días. Beber abundantes líquidos.",
                listOf(
                    Medicamento("Paracetamol 500mg", "1 tableta cada 8 horas por 3 días"),
                    Medicamento("Ibuprofeno 400mg", "1 tableta cada 12 horas si hay dolor")
                )
            )
        )

        binding.recyclerDiagnosticos.layoutManager = LinearLayoutManager(this)
        binding.recyclerDiagnosticos.adapter = DiagnosticoAdapter(listaDemo)

        binding.btnRegresar.setOnClickListener {
            finish()
        }
    }

    inner class DiagnosticoAdapter(private val diagnosticos: List<Diagnostico>) : RecyclerView.Adapter<DiagnosticoAdapter.ViewHolder>() {
        inner class ViewHolder(val itemBinding: CuadroDiagnosticoBinding) : RecyclerView.ViewHolder(itemBinding.root) {
            fun bind(diag: Diagnostico) {
                itemBinding.txtTituloDiagnostico.text = diag.titulo
                itemBinding.txtDoctorYFecha.text = diag.doctorFecha
                itemBinding.txtRecomendaciones.text = diag.recomendaciones

                itemBinding.layoutMedicamentos.removeAllViews()

                diag.medicamentos.forEach { med ->
                    val textMed = TextView(itemView.context).apply {
                        text = "• ${med.nombre} - ${med.dosis}"
                        setTextColor(android.graphics.Color.parseColor("#424242"))
                        textSize = 14f
                    }
                    itemBinding.layoutMedicamentos.addView(textMed)
                }
            }
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
            CuadroDiagnosticoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
        override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(diagnosticos[position])
        override fun getItemCount() = diagnosticos.size
    }
}