package ec.edu.mapsalud.medicPages

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
import ec.edu.mapsalud.databinding.MedicPrincipalBinding
import ec.edu.mapsalud.dto.Medico
import android.graphics.Color
import ec.edu.mapsalud.utils.ThemeUtils


class PrincipalMedic : AppCompatActivity() {

    private lateinit var binding: MedicPrincipalBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeUtils.applyTheme(this)
        super.onCreate(savedInstanceState)
        binding = MedicPrincipalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        actualizarEstiloMenu(R.id.btnMenuCitasMedicas)
        cargarDatosDoctor()

        if (savedInstanceState == null) {
            cambiarFragment(CitasMedicasFragment())
        }

        initListeners()
    }

    private fun cargarDatosDoctor() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("usuarios").document(uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val medico = document.toObject(Medico::class.java)
                    binding.txtBienvenidaMedic.text = "Buen día, Dr. ${medico?.info?.apellidos ?: ""}"
                }
            }.addOnFailureListener {
                binding.txtBienvenidaMedic.text = "Buen día, Doctor"
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