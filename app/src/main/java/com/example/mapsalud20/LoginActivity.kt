package com.example.mapsalud20

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mapsalud20.databinding.ActivityLoginBinding
import com.example.mapsalud20.medicPages.PrincipalMedic
import com.example.mapsalud20.userPages.PrincipalUser


class LoginActivity : AppCompatActivity() {

    lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (verificarSesionGuardada()) {
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initListeners()
    }

    private fun verificarSesionGuardada(): Boolean {
        val sharedPref = getSharedPreferences("MapSaludPrefs", Context.MODE_PRIVATE)
        val isLogged = sharedPref.getBoolean("IS_LOGGED", false)

        if (isLogged) {
            val tipoUsuario = sharedPref.getString("TIPO_USUARIO", "")

            if (tipoUsuario == "PACIENTE") {
                startActivity(Intent(this, PrincipalUser::class.java))
                finish()
                return true
            } else if (tipoUsuario == "DOCTOR") {
                startActivity(Intent(this, PrincipalMedic::class.java))
                finish()
                return true
            }
        }
        return false
    }

    private fun initListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.txtEmail.text.toString()
            val pass = binding.txtPassword.text.toString()

            if (email == "paciente@gmail.com" && pass == "1234") {
                val intent = Intent(this, PrincipalUser::class.java)
                intent.putExtra("TIPO_USUARIO", "PACIENTE")
                startActivity(intent)
                finish()
            } else if (email == "doctor@gmail.com" && pass == "1234") {
                val intent = Intent(this, PrincipalMedic::class.java)
                intent.putExtra("TIPO_USUARIO", "DOCTOR")
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "Correo o contraseña incorrectos", Toast.LENGTH_SHORT).show()
            }
        }

        binding.txtIrRegistro.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    private fun guardarSesion(tipoUsuario: String) {
        val sharedPref = getSharedPreferences("MapSaludPrefs", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.putBoolean("IS_LOGGED", true)
        editor.putString("TIPO_USUARIO", tipoUsuario)
        editor.apply()
    }
}