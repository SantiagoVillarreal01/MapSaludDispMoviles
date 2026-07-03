package ec.edu.mapsalud.medicPages

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import ec.edu.mapsalud.ConfiguracionApp
import ec.edu.mapsalud.EditarPerfil
import ec.edu.mapsalud.R
import ec.edu.mapsalud.databinding.MedicPrincipalBinding
import android.graphics.Color
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import ec.edu.mapsalud.remote.impl.UsuariosRepositoryImpl
import ec.edu.mapsalud.remote.inter.UsuariosRepository
import ec.edu.mapsalud.usercases.usuariosUC.GetDoctorByIdUC
import ec.edu.mapsalud.utils.ThemeUtils
import ec.edu.mapsalud.viewmodel.UsuarioViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class PrincipalMedic : AppCompatActivity() {

    private lateinit var binding: MedicPrincipalBinding
    private val auth = FirebaseAuth.getInstance()
    private val usuarioVM by viewModels<UsuarioViewModel>()
    private val usuarioRepository = UsuariosRepositoryImpl()

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeUtils.applyTheme(this)
        super.onCreate(savedInstanceState)
        binding = MedicPrincipalBinding.inflate(layoutInflater)
        setContentView(binding.root)
        actualizarEstiloMenu(R.id.btnMenuCitasMedicas)

        initObservers()
        cargarDatosDoctor()

        if (savedInstanceState == null) {
            cambiarFragment(CitasMedicasFragment())
        }

        initListeners()
    }

    private fun cargarDatosDoctor() {
        val uid = auth.currentUser?.uid ?: return
        usuarioVM.cargarMedico(
            idDoctor = uid,
            getDoctorByIdUC = GetDoctorByIdUC(usuarioRepository)
        )
    }

    private fun initObservers() {
        usuarioVM.medico.observe(this) { medico ->
            if (medico != null) {
                binding.txtBienvenidaMedic.text = "Buen día, Dr. ${medico.info.apellidos}"
            } else {
                binding.txtBienvenidaMedic.text = "Buen día, Doctor"
            }
        }
    }

    private fun actualizarEstiloMenu(idBotonSeleccionado: Int) {
        val botones = listOf(
            binding.btnMenuCitasMedicas,
            binding.btnMenuConsultorio,
            binding.btnMenuHorarios,
            binding.btnMenuDiagnosticosMedic
        )

        for (boton in botones) {
            val estaSeleccionado = (boton.id == idBotonSeleccionado)
            boton.isSelected = estaSeleccionado

            val colorFrente = if (estaSeleccionado) {
                Color.WHITE
            } else {
                Color.parseColor("#5F6368")
            }

            for (i in 0 until boton.childCount) {
                val vistaHijo = boton.getChildAt(i)
                if (vistaHijo is ImageView) {
                    vistaHijo.setColorFilter(colorFrente)
                } else if (vistaHijo is TextView) {
                    vistaHijo.setTextColor(colorFrente)
                }
            }
        }
    }

    private fun initListeners() {
        binding.imgProfileMedic.setOnClickListener {
            startActivity(Intent(this, EditarPerfil::class.java))
        }
        binding.btnConfiguracionMedic.setOnClickListener {
            startActivity(Intent(this, ConfiguracionApp::class.java))
        }
        binding.btnMenuCitasMedicas.setOnClickListener {
            actualizarEstiloMenu(it.id)
            cambiarFragment(CitasMedicasFragment())
        }
        binding.btnMenuConsultorio.setOnClickListener {
            actualizarEstiloMenu(it.id)
            startActivity(Intent(this, AgregarConsultorio::class.java))
        }
        binding.btnMenuHorarios.setOnClickListener {
            actualizarEstiloMenu(it.id)
            cambiarFragment(EditarHorariosFragment())
        }
        binding.btnMenuDiagnosticosMedic.setOnClickListener {
            actualizarEstiloMenu(it.id)
            cambiarFragment(DiagnosticosFragment())
        }
    }

    private fun cambiarFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentContainerMedic.id, fragment)
            .commit()
    }
}