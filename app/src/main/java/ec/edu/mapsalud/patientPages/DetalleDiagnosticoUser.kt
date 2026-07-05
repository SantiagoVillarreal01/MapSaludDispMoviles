package ec.edu.mapsalud.patientPages

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import ec.edu.mapsalud.databinding.UserDetalleDiagnosticoBinding
import ec.edu.mapsalud.utils.ThemeUtils

class DetalleDiagnosticoUser : AppCompatActivity() {

    private lateinit var binding: UserDetalleDiagnosticoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeUtils.applyTheme(this)
        super.onCreate(savedInstanceState)
        binding = UserDetalleDiagnosticoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val doctor = intent.getStringExtra("DOCTOR_NAME") ?: "Médico"
        val fecha = intent.getStringExtra("FECHA") ?: ""
        val diagnostico = intent.getStringExtra("DIAGNOSTICO") ?: "No especificado"
        val tratamiento = intent.getStringExtra("TRATAMIENTO") ?: "No especificado"
        val sugerencias = intent.getStringExtra("SUGERENCIAS") ?: "No especificado"

        binding.txtDoctorNombre.text = doctor
        binding.txtFechaDetalle.text = "📅 $fecha"
        binding.txtDiagnosticoDetalle.text = diagnostico
        binding.txtTratamientoDetalle.text = tratamiento
        binding.txtSugerenciasDetalle.text = sugerencias

        binding.btnRegresar.setOnClickListener { finish() }
    }
}
