package com.example.mapsalud.medicPages

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast
import ec.edu.mapsalud.databinding.MedicDetalleCitaBinding

class DetalleCitaMedic : AppCompatActivity() {

    private lateinit var binding: MedicDetalleCitaBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MedicDetalleCitaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val nombre = intent.getStringExtra("NOMBRE_PACIENTE") ?: "Paciente"
        binding.txtNombreDetalle.text = nombre

        binding.btnRegresar.setOnClickListener {
            finish()
        }

        binding.btnGuardarEstado.setOnClickListener {

            val estadoSeleccionado = when (binding.radioGroupEstado.checkedRadioButtonId) {
                binding.radioAtendida.id -> "Atendida"
                binding.radioInasistencia.id -> "Inasistencia"
                binding.radioCancelada.id -> "Cancelada"
                else -> null
            }

            if (estadoSeleccionado != null) {
                Toast.makeText(this, "Estado actualizado a: $estadoSeleccionado", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Por favor selecciona un estado", Toast.LENGTH_SHORT).show()
            }
        }
    }
}