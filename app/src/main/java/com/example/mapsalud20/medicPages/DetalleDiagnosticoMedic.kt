package com.example.mapsalud20.medicPages

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.mapsalud20.R
import com.example.mapsalud20.databinding.MedicDetalleDiagnosticoBinding
import android.widget.Toast

class DetalleDiagnosticoMedic : AppCompatActivity() {

    private lateinit var binding: MedicDetalleDiagnosticoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MedicDetalleDiagnosticoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val nombrePaciente = intent.getStringExtra("PACIENTE_NOMBRE") ?: "Desconocido"
        val motivoCita = intent.getStringExtra("MOTIVO_CITA") ?: "Sin motivo"

        binding.txtPacienteDetalle.text = "Paciente: $nombrePaciente"
        binding.txtMotivoDetalle.text = "Motivo: $motivoCita"

        binding.btnRegresarDiag.setOnClickListener {
            finish()
        }

        binding.btnEnviarDiagnostico.setOnClickListener {
            val diagnostico = binding.inputDiagnostico.text.toString()
            val receta = binding.inputReceta.text.toString()

            if (diagnostico.isEmpty()) {
                Toast.makeText(this, "El diagnóstico es obligatorio", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Diagnóstico enviado exitosamente", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }
}