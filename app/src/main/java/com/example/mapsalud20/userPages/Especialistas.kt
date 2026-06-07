package com.example.mapsalud20.userPages

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mapsalud20.databinding.UserEspecialistasBinding
import com.example.mapsalud20.databinding.CuadroDoctorBinding
import com.example.mapsalud20.dto.Medico

class Especialistas : AppCompatActivity() {

    private lateinit var binding: UserEspecialistasBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = UserEspecialistasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configurarRecyclerView()
    }

    private fun configurarRecyclerView() {
        val listaMedicosDemo = listOf(
            Medico(
                nombres = "Andrés",
                apellidos = "Gomez",
                correo = "andres.gomez@mail.com",
                correoMapsalud = "agomez@mapsalud.com",
                contrasenaMapsalud = "123456",
                telefono = "0999999999",
                cedula = "1712345678",
                idEspecialidadPrincipal = 1,
                anosExperiencia = 15
            ),
            Medico(
                nombres = "Elena",
                apellidos = "Rivas",
                correo = "elena.rivas@mail.com",
                correoMapsalud = "erivas@mapsalud.com",
                contrasenaMapsalud = "123456",
                telefono = "0988888888",
                cedula = "1787654321",
                idEspecialidadPrincipal = 1,
                anosExperiencia = 8
            )
        )

        val doctorAdapter = DoctorAdapter(listaMedicosDemo) { medicoSeleccionado ->
            val intent = Intent(this, ReservarCita::class.java)
            intent.putExtra("DOCTOR_NAME", "${medicoSeleccionado.nombres} ${medicoSeleccionado.apellidos}")
            startActivity(intent)
        }

        binding.recyclerViewEspecialistas.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewEspecialistas.adapter = doctorAdapter
    }


    inner class DoctorAdapter(
        private val medicos: List<Medico>,
        private val onReservarClick: (Medico) -> Unit
    ) : RecyclerView.Adapter<DoctorAdapter.DoctorViewHolder>() {

        inner class DoctorViewHolder(val binding: CuadroDoctorBinding) : RecyclerView.ViewHolder(binding.root) {
            fun bind(medico: Medico) {
                binding.txtNombreDoctor.text = "Dr. ${medico.nombres} ${medico.apellidos}"

                binding.txtEspecialidadDoctor.text = when(medico.idEspecialidadPrincipal) {
                    1 -> "Cardiología Intervencionista"
                    2 -> "Neurología"
                    3 -> "Pediatría"
                    else -> "Cardiología General"
                }

                binding.txtRating.text = "★ 4.9 (124)"
                binding.txtDisponibilidad.text = "📅 Disponible hoy"
                binding.txtExperiencia.text = "💼 ${medico.anosExperiencia} años de experiencia"

                binding.btnReservar.setOnClickListener {
                    onReservarClick(medico)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DoctorViewHolder {
            val binding = CuadroDoctorBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return DoctorViewHolder(binding)
        }

        override fun onBindViewHolder(holder: DoctorViewHolder, position: Int) {
            holder.bind(medicos[position])
        }

        override fun getItemCount() = medicos.size
    }
}