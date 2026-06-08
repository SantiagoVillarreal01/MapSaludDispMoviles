package com.example.mapsalud.medicPages

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast
import ec.edu.mapsalud.databinding.MedicDetalleDiagnosticoBinding

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