package com.example.mapsalud20.medicPages

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.mapsalud20.R
import androidx.fragment.app.Fragment
import com.example.mapsalud20.ConfiguracionApp
import com.example.mapsalud20.EditarPerfil
import com.example.mapsalud20.databinding.MedicPrincipalBinding

class PrincipalMedic : AppCompatActivity() {

    private lateinit var binding: MedicPrincipalBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MedicPrincipalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            cambiarFragment(CitasMedicasFragment())
        }

        initListeners()
    }

    private fun initListeners() {

        binding.imgProfileMedic.setOnClickListener {
            startActivity(Intent(this, EditarPerfil::class.java))
        }

        binding.btnConfiguracionMedic.setOnClickListener {
            startActivity(Intent(this, ConfiguracionApp::class.java))
        }

        binding.btnMenuCitasMedicas.setOnClickListener {
            cambiarFragment(CitasMedicasFragment())
        }

        binding.btnMenuConsultorio.setOnClickListener {
            startActivity(Intent(this, AgregarConsultorio::class.java))
        }

        binding.btnMenuHorarios.setOnClickListener {
            cambiarFragment(EditarHorariosFragment())
        }

        binding.btnMenuDiagnosticosMedic.setOnClickListener {
            cambiarFragment(DiagnosticosFragment())
        }
    }

    private fun cambiarFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentContainerMedic.id, fragment)
            .commit()
    }
}