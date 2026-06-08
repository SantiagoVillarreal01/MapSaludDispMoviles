package com.example.mapsalud.userPages

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.mapsalud.databinding.UserReservarCitaBinding

class ReservarCita : AppCompatActivity() {
    private lateinit var binding: UserReservarCitaBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = UserReservarCitaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val doctorName = intent.getStringExtra("DOCTOR_NAME")
        if (!doctorName.isNullOrEmpty()) {
            binding.txtNombreDoctor.text = doctorName
        }

        binding.btnRegresar.setOnClickListener {
            finish()
        }

        binding.btnConfirmarCita.setOnClickListener {
            val intent = Intent(this, PrincipalUser::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            intent.putExtra("MENSAJE_SNACKBAR", "Cita creada exitosamente")
            startActivity(intent)
            finish()
        }
    }
}