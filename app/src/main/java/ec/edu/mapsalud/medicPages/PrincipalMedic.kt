package ec.edu.mapsalud.medicPages

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import ec.edu.mapsalud.ConfiguracionApp
import ec.edu.mapsalud.EditarPerfil
import ec.edu.mapsalud.databinding.MedicPrincipalBinding

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