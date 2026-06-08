package com.example.mapsalud

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import ec.edu.mapsalud.databinding.ActivityConfiguracionAppBinding

class ConfiguracionApp : AppCompatActivity() {

    private lateinit var binding: ActivityConfiguracionAppBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConfiguracionAppBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnRegresar.setOnClickListener {
            finish()
        }

        configurarEncabezadoUsuario()
        initListeners()
    }

    private fun configurarEncabezadoUsuario() {
        val sharedPref = getSharedPreferences("MapSaludPrefs", Context.MODE_PRIVATE)
        val tipoUsuario = sharedPref.getString("TIPO_USUARIO", "PACIENTE")

        if (tipoUsuario == "DOCTOR") {
            binding.txtConfigNombreUsuario.text = "Dr. Alejandro Méndez"
            binding.txtConfigDetalleUsuario.text = "Cardiólogo | ID: 29481-MS"
        } else {
            // Valores en caso de que entre un Paciente
            binding.txtConfigNombreUsuario.text = "Adriana Martínez"
            binding.txtConfigDetalleUsuario.text = "Paciente Asegurado | Nvl. Oro"
        }
    }

    private fun initListeners() {
        binding.itemPreferencias.setOnClickListener {
            Toast.makeText(this, "Abriendo Preferencias", Toast.LENGTH_SHORT).show()
        }

        binding.itemNotificaciones.setOnClickListener {
            Toast.makeText(this, "Configuración de Notificaciones", Toast.LENGTH_SHORT).show()
        }

        binding.btnCerrarSesion.setOnClickListener {
            val sharedPref = getSharedPreferences("MapSaludPrefs", Context.MODE_PRIVATE)
            sharedPref.edit().clear().apply()

            Toast.makeText(this, "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show()

            val intent = Intent(this, LoginScreen::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)

            finish()
        }
    }
}