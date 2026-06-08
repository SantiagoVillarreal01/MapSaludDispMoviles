package ec.edu.mapsalud

import android.os.Bundle
import android.util.Patterns
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import ec.edu.mapsalud.datos.FirebaseManager
import com.google.android.material.snackbar.Snackbar
import ec.edu.mapsalud.databinding.ActivityForgotPasswordBinding

class ForgotPasswordScreen : AppCompatActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initListeners()
    }

    private fun initListeners() {
        binding.btnBackArrow.setOnClickListener {
            finish()
        }

        binding.btnResetPassword.setOnClickListener {
            val email = binding.txtEmailReset.text.toString().trim()

            if (email.isEmpty()) {
                showMessage("Por favor, ingresa tu correo electrónico")
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                showMessage("Por favor, ingresa un correo electrónico válido")
                return@setOnClickListener
            }

            setLoadingState(true)

            FirebaseManager.auth.sendPasswordResetEmail(email)
                .addOnSuccessListener {
                    setLoadingState(false)
                    showMessage("¡Correo Enviado! Revisa tu bandeja de entrada.")

                    binding.root.postDelayed({
                        finish()
                    }, 2000)
                }
                .addOnFailureListener { exception ->
                    setLoadingState(false)
                    showMessage("Error: ${exception.localizedMessage}")
                }
        }
    }


    private fun setLoadingState(isLoading: Boolean) {
        if (isLoading) {
            binding.btnResetPassword.isEnabled = false
            binding.btnResetPassword.text = ""
            binding.btnResetPassword.setIconResource(0)
            binding.progressBarReset.visibility = View.VISIBLE
        } else {
            binding.btnResetPassword.isEnabled = true
            binding.btnResetPassword.text = "Enviar Instrucciones"
            binding.btnResetPassword.setIconResource(android.R.drawable.ic_menu_send)
            binding.progressBarReset.visibility = View.GONE
        }
    }

    private fun showMessage(message: String) {
        val snackbar = Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
        snackbar.setBackgroundTint(getColor(R.color.black_soft))
        snackbar.setTextColor(getColor(R.color.white))
        snackbar.show()
    }
}