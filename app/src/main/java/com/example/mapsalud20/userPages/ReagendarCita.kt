package com.example.mapsalud20.userPages

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.mapsalud20.databinding.UserReagendarCitaBinding

class ReagendarCita : AppCompatActivity() {
    private lateinit var binding: UserReagendarCitaBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = UserReagendarCitaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnRegresar.setOnClickListener {
            finish()
        }

        binding.btnReagendarCita.setOnClickListener {
            val intent = Intent(this, PrincipalUser::class.java)

            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            intent.putExtra("MENSAJE_SNACKBAR", "Cita reagendada con éxito")
            startActivity(intent)
            finish()

        }
    }
}