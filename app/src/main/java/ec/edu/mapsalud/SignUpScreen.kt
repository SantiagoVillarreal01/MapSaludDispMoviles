package ec.edu.mapsalud

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import androidx.appcompat.app.AppCompatActivity
import ec.edu.mapsalud.datos.FirebaseManager
import com.google.android.material.snackbar.Snackbar
import ec.edu.mapsalud.databinding.ActivitySignUpPageBinding


class SignUpScreen : AppCompatActivity() {

    lateinit var binding: ActivitySignUpPageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpPageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initVariables()
        initListeners()
    }

    var counter: Int = 0


    private fun initVariables() {
        counter = 1
    }

    private fun initListeners() {
        binding.btnSignUp.setOnClickListener {

            if (!validateFields()) {
                return@setOnClickListener
            }

            registerUser()
        }

        binding.txtAlradyHaveAccount.setOnClickListener {
            val intent = Intent(this, LoginScreen::class.java)
            startActivity(intent)
            finish()
        }
    }


    private fun registerUser() {

        val name = binding.name.text.toString().trim()
        val idNumber = binding.idNumber.text.toString().trim()
        val email = binding.email.text.toString().trim()
        val password = binding.password.text.toString()

        binding.btnSignUp.isEnabled = false

        FirebaseManager.auth.createUserWithEmailAndPassword(
            email,
            password
        )
            .addOnSuccessListener { result ->

                val firebaseUser = result.user
                binding.btnSignUp.isEnabled = true

                if (firebaseUser == null) {

                    showMessage(
                        "Error al obtener el usuario creado"
                    )

                    return@addOnSuccessListener
                }


                val userData = hashMapOf(
                    "uid" to firebaseUser.uid,
                    "name" to name,
                    "idNumber" to idNumber,
                    "email" to email
                )

                FirebaseManager.db
                    .collection("users")
                    .document(firebaseUser.uid)
                    .set(userData)
                    .addOnSuccessListener {
                        binding.btnSignUp.isEnabled = true

                        firebaseUser.sendEmailVerification()
                            .addOnSuccessListener {

                                showMessage("Correo de verificación enviado")
                            }
                            .addOnFailureListener {

                                showMessage("Error al registrar usuario")
                            }
                    }
                    .addOnFailureListener {
                        binding.btnSignUp.isEnabled = true

                        showMessage("Error Firestore: ${it.message}")
                    }

            }
            .addOnFailureListener { exception ->

                binding.btnSignUp.isEnabled = true

                when {

                    exception.message?.contains(
                        "email address is already in use",
                        true
                    ) == true -> {

                        showMessage(
                            "Este correo ya está registrado"
                        )
                    }

                    else -> {

                        showMessage(
                            exception.message
                                ?: "Error al registrar usuario"
                        )
                    }
                }
            }
    }

//    private fun showVerificationDialog() {
//
//        val view = layoutInflater.inflate(
//            R.layout.activity_verification_code_dialog,
//            null
//        )
//
//        val dialog = MaterialAlertDialogBuilder(this)
//            .setView(view)
//            .create()
//
//        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
//
//        val pinView = view.findViewById<PinView>(R.id.pinView)
//
//        val btnCancel = view.findViewById<MaterialButton>(R.id.btnCancel)
//
//        val btnVerify = view.findViewById<MaterialButton>(R.id.btnVerify)
//
//        btnCancel.setOnClickListener {
//            dialog.dismiss()
//        }
//
//        btnVerify.setOnClickListener {
//
//            if (pinView.text.toString().length == 6) {
//
//                dialog.dismiss()
//
//                val code = pinView.text.toString()
//
//                Toast.makeText(this, code, Toast.LENGTH_SHORT).show()
//            }
//        }
//
//        dialog.show()
//        pinView.requestFocus()
//
//        val imm = getSystemService(
//            INPUT_METHOD_SERVICE
//        ) as InputMethodManager
//
//        imm.showSoftInput(
//            pinView,
//            InputMethodManager.SHOW_IMPLICIT
//        )
//        pinView.requestFocus()
//    }
//


    private fun validateFields(): Boolean {

        val name = binding.name.text.toString().trim()
        val idNumber = binding.idNumber.text.toString().trim()
        val email = binding.email.text.toString().trim()
        val password = binding.password.text.toString()

        if (name.isEmpty()) {
            showMessage("Ingrese su nombre")
            return false
        }

        if (idNumber.isEmpty()) {
            showMessage("Ingrese su cédula")
            return false
        }

        if (email.isEmpty()) {
            showMessage("Ingrese un correo")
            return false
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showMessage("Correo inválido")
            return false
        }

        if (!idNumber.matches(Regex("\\d{10}"))) {
            showMessage("Ingrese una cédula válida")
            return false
        }
        if (password.length < 6) {
            showMessage("La contraseña debe tener al menos 6 caracteres")
            return false
        }

        if (!binding.terms.isChecked) {
            showMessage("Debe aceptar los términos y condiciones")
            return false
        }

        return true
    }

    private fun showMessage(message: String) {

        val snackbar = Snackbar.make(
            binding.root,
            message,
            Snackbar.LENGTH_LONG
        )

        snackbar.setBackgroundTint(
            getColor(R.color.black_soft)
        )

        snackbar.setTextColor(
            getColor(R.color.white)
        )

        snackbar.show()
    }
}