package ec.edu.mapsalud.patientPages

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
import ec.edu.mapsalud.databinding.UserPrincipalBinding
import android.graphics.Color
import androidx.activity.viewModels
import com.bumptech.glide.Glide
import ec.edu.mapsalud.remote.impl.UsuariosRepositoryImpl
import ec.edu.mapsalud.usercases.usuariosUC.GetPacienteByIdUC
import ec.edu.mapsalud.utils.ThemeUtils
import ec.edu.mapsalud.viewmodel.UsuarioViewModel
class PrincipalPatient : AppCompatActivity() {

    private lateinit var binding: UserPrincipalBinding
    private val auth = FirebaseAuth.getInstance()
    private val userVM by viewModels<UsuarioViewModel>()
    private val usuarioRepository = UsuariosRepositoryImpl()

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeUtils.applyTheme(this)
        super.onCreate(savedInstanceState)
        binding = UserPrincipalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        actualizarEstiloMenu(R.id.btnMenuNuevaCita)
        initObservers()
        cargarDatosUsuario()

        if (savedInstanceState == null) {
            cambiarFragment(NuevaCitaFragment())
        }

        initListeners()
    }

    override fun onResume() {
        super.onResume()
        cargarFotoPerfilDesdeCache()
    }

    private fun cargarFotoPerfilDesdeCache() {
        val sharedPref = getSharedPreferences("MapSaludCache", MODE_PRIVATE)
        val fotoUrl = sharedPref.getString("USER_FOTO_URL", "") ?: ""

        Glide.with(this).clear(binding.imgProfile)

        if (fotoUrl.isNotEmpty()) {
            Glide.with(this)
                .load(fotoUrl)
                .centerCrop()
                .placeholder(R.drawable.user)
                .error(R.drawable.user)
                .into(binding.imgProfile)
        } else {
            binding.imgProfile.setImageResource(R.drawable.user)
        }
    }

    private fun cargarDatosUsuario() {
        val uid = auth.currentUser?.uid ?: return
        userVM.cargarPaciente(
            idUser = uid,
            getPacienteByIdUC = GetPacienteByIdUC(usuarioRepository)
        )
    }

    private fun actualizarEstiloMenu(idBotonSeleccionado: Int) {
        val botones = listOf(
            binding.btnMenuNuevaCita,
            binding.btnMenuReagendar,
            binding.btnMenuDiagnosticos,
            binding.btnMenuCancelar
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
        binding.imgProfile.setOnClickListener {
            startActivity(Intent(this, EditarPerfil::class.java))
        }
        binding.btnConfiguracion.setOnClickListener {
            startActivity(Intent(this, ConfiguracionApp::class.java))
        }
        binding.btnMenuNuevaCita.setOnClickListener {
            actualizarEstiloMenu(it.id)
            cambiarFragment(NuevaCitaFragment())
        }
        binding.btnMenuReagendar.setOnClickListener {
            actualizarEstiloMenu(it.id)
            cambiarFragment(ReagendarCitaFragment())
        }
        binding.btnMenuDiagnosticos.setOnClickListener {
            actualizarEstiloMenu(it.id)
            cambiarFragment(DiagnosticosFragment())
        }
        binding.btnMenuCancelar.setOnClickListener {
            actualizarEstiloMenu(it.id)
            cambiarFragment(CancelarCitaFragment())
        }
    }

    fun cambiarFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentContainerUser.id, fragment)
            .commit()
    }

    private fun initObservers() {
        userVM.paciente.observe(this) { paciente ->
            if (paciente != null) {

                val nombres = paciente.info.nombres.trim()
                val apellidos = paciente.info.apellidos.trim()
                val primerNombre = nombres.split(" ").firstOrNull() ?: ""
                val primerApellido = apellidos.split(" ").firstOrNull() ?: ""
                binding.txtUserName.text = "$primerNombre $primerApellido"

                val fotoUrlActual = paciente.info.imageUrl ?: ""
                val sharedPref = getSharedPreferences("MapSaludCache", MODE_PRIVATE)
                sharedPref.edit().putString("USER_FOTO_URL", fotoUrlActual).apply()

                Glide.with(this).clear(binding.imgProfile)
                if (fotoUrlActual.isNotEmpty()) {
                    Glide.with(this)
                        .load(fotoUrlActual)
                        .centerCrop()
                        .placeholder(R.drawable.user)
                        .error(R.drawable.user)
                        .into(binding.imgProfile)
                } else {
                    binding.imgProfile.setImageResource(R.drawable.user)
                }

            } else {
                binding.txtUserName.text = "Paciente"
                binding.imgProfile.setImageResource(R.drawable.user)
            }
        }
    }
}
