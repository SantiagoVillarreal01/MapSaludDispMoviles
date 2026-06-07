package com.example.mapsalud20

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mapsalud20.databinding.ActivityRegisterBinding

class RegisterActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {

    lateinit var binding: ActivityRegisterBinding

    // Código maestro de validación para doctores
    private val CODIGO_SECRETO_DOCTOR = "DOC-2026"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initVariables()
        initListeners()
    }

    private fun initVariables() {
        val opcionesRol = listOf("Paciente", "Médico")
        val adapterRol = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, opcionesRol)
        binding.spinnerRol.apply {
            adapter = adapterRol
            onItemSelectedListener = this@RegisterActivity
        }

        val opcionesEspecialidad = listOf("Cardiología", "Neurología", "Pediatría", "Dermatología")
        val adapterEsp = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, opcionesEspecialidad)
        binding.spinnerEspecialidad.adapter = adapterEsp
    }

    private fun initListeners() {
        binding.btnRegistrar.setOnClickListener {
            val rolSeleccionado = binding.spinnerRol.selectedItem.toString()

            if (rolSeleccionado == "Médico") {
                val codigoIngresado = binding.txtCodigoDoctor.text.toString()
                if (codigoIngresado != CODIGO_SECRETO_DOCTOR) {
                    Toast.makeText(this, "Código de Doctor inválido. No puedes crear una cuenta médica.", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
            }


            Toast.makeText(this, "Cuenta de $rolSeleccionado creada exitosamente", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val rolSeleccionado = parent?.getItemAtPosition(position).toString()

        if (rolSeleccionado == "Médico") {
            binding.layoutDoctor.visibility = View.VISIBLE
        } else {
            binding.layoutDoctor.visibility = View.GONE
        }
    }

    override fun onNothingSelected(p0: AdapterView<*>?) {

    }
}