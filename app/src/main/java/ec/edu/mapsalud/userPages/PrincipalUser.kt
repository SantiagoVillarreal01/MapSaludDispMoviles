package ec.edu.mapsalud.userPages

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import ec.edu.mapsalud.ConfiguracionApp
import ec.edu.mapsalud.EditarPerfil
import ec.edu.mapsalud.R
import ec.edu.mapsalud.databinding.UserPrincipalBinding
import ec.edu.mapsalud.dto.Paciente
import android.graphics.Color
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import ec.edu.mapsalud.utils.ThemeUtils

class PrincipalUser : AppCompatActivity() {

    private lateinit var binding: UserPrincipalBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeUtils.applyTheme(this)
        super.onCreate(savedInstanceState)
        binding = UserPrincipalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        actualizarEstiloMenu(R.id.btnMenuNuevaCita)
        cargarDatosUsuario()

        if (savedInstanceState == null) {
            cambiarFragment(NuevaCitaFragment())
        }

        initListeners()
    }

    private fun cargarDatosUsuario() {
        val uid = auth.currentUser?.uid ?: return
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val document = db.collection("usuarios").document(uid).get().await()
                val paciente = document.toObject(Paciente::class.java)
                withContext(Dispatchers.Main) {
                    if (paciente != null) {
                        val nombres = paciente.info.nombres.trim()
                        val apellidos = paciente.info.apellidos.trim()
                        val primerNombre = nombres.split(" ").firstOrNull() ?: ""
                        val primerApellido = apellidos.split(" ").firstOrNull() ?: ""
                        binding.txtUserName.text = "$primerNombre $primerApellido"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.txtUserName.text = "Paciente"
                }
            }
        }
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
}
