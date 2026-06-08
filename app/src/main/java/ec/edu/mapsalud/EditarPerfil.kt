package com.example.mapsalud

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import ec.edu.mapsalud.databinding.ActivityEditarPerfilBinding

class EditarPerfil : AppCompatActivity() {

    private lateinit var binding: ActivityEditarPerfilBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditarPerfilBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnRegresar.setOnClickListener {
            finish()
        }

        cargarDatosActuales()
        configurarListeners()
    }

    private fun cargarDatosActuales() {
        val sharedPref = getSharedPreferences("MapSaludPrefs", Context.MODE_PRIVATE)
        val tipoUsuario = sharedPref.getString("TIPO_USUARIO", "PACIENTE")

        if (tipoUsuario == "DOCTOR") {
            binding.inputNombre.setText("Carlos")
            binding.inputApellido.setText("Javier")
            binding.inputCorreo.setText("doctor1@gmail.com")
            binding.inputTelefono.setText("+34 600 111 222")
        } else {
            binding.inputNombre.setText("Adriana")
            binding.inputApellido.setText("Martínez")
            binding.inputCorreo.setText("a.martinez@mapsalud.com")
            binding.inputTelefono.setText("+34 612 345 678")
        }
    }

    private fun configurarListeners() {
        binding.btnCambiarFoto.setOnClickListener {
            Toast.makeText(this, "Funcionalidad para abrir la galería próximamente", Toast.LENGTH_SHORT).show()
        }

        binding.btnGuardarCambios.setOnClickListener {
            val nombre = binding.inputNombre.text.toString().trim()
            val apellido = binding.inputApellido.text.toString().trim()
            val correo = binding.inputCorreo.text.toString().trim()
            val telefono = binding.inputTelefono.text.toString().trim()

            if (nombre.isEmpty() || apellido.isEmpty() || correo.isEmpty() || telefono.isEmpty()) {
                Toast.makeText(this, "Por favor, llena todos los campos", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Cambios guardados con éxito", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }
}