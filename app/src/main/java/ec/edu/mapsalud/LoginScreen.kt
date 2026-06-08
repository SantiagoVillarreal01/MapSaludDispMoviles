package com.example.mapsalud

import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.mapsalud.datos.FirebaseManager
import com.example.mapsalud.enum.Type
import com.example.mapsalud.medicPages.PrincipalMedic
import com.example.mapsalud.userPages.PrincipalUser
import com.example.mapsalud.utils.EmailJSRequest
import com.example.mapsalud.utils.RetrofitClient
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.GoogleAuthProvider
import ec.edu.mapsalud.R
import ec.edu.mapsalud.databinding.ActivityLoginPageBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LoginScreen : AppCompatActivity() {

    var counter: Int = 0
    var type: Type? = null
    private lateinit var binding: ActivityLoginPageBinding
    private lateinit var googleSignInClient: GoogleSignInClient

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                account?.idToken?.let { idToken ->
                    firebaseAuthWithGoogle(idToken)
                }
            } catch (e: ApiException) {
                showMessage("Error de conexión con Google: ${e.localizedMessage}")
            }
        } else {
            binding.btnGoogle.isEnabled = true
            showMessage("Inicio de sesión cancelado. Código de resultado: ${result.resultCode}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initGoogleConfig()
        initListeners()
        initVariables()
    }

    private fun initVariables() {
        type = Type.PATIENT
    }

    private fun initGoogleConfig() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("861655205039-e0sjvj2qsmnefevofqlr49uf8tv1tc76.apps.googleusercontent.com")
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun initListeners() {
        binding.btnSignin.setOnClickListener {
            loginUser()
        }

        binding.btnGoogle.setOnClickListener {
            signInWithGoogle()
        }
        binding.btnDoctor.setOnClickListener {
            type = Type.DOCTOR
            updateSelection()
        }

        binding.btnPatient.setOnClickListener {
            type = Type.PATIENT
            updateSelection()
        }

        binding.txtCreateAccount.setOnClickListener {
            val intent = Intent(this, SignUpScreen::class.java)
            startActivity(intent)
            finish()
        }

        binding.forgotPassword.setOnClickListener {
            val intent = Intent(this, ForgotPasswordScreen::class.java)
            startActivity(intent)
        }
    }

    private fun loginUser() {

        val email = binding.txtEmail.text.toString().trim()
        val password = binding.txtPassword.text.toString()

        if (email.isEmpty()) {
            showMessage("Ingrese un correo")
            return
        }

        if (password.isEmpty()) {
            showMessage("Ingrese una contraseña")
            return
        }

        binding.btnSignin.isEnabled = false

        FirebaseManager.auth.signInWithEmailAndPassword(
            email,
            password
        )
            .addOnSuccessListener {

                val user = FirebaseManager.auth.currentUser

                user?.reload()?.addOnSuccessListener {

                    if (user.isEmailVerified) {

                      navegarAlHome(Type.PATIENT == type)

                    } else {

                        FirebaseManager.auth.signOut()

                        showMessage(
                            "Debe verificar su correo electrónico"
                        )
                    }

                    binding.btnSignin.isEnabled = true
                }
            }
            .addOnFailureListener {

                binding.btnSignin.isEnabled = true
                counter++
                if (counter >= 3) {
                    sendSecurityAlert(email)
                }
                showMessage(
                    "Correo o contraseña incorrectos"
                )
            }
    }

    private fun updateSelection() {

        if (type == Type.PATIENT) {

            binding.btnPatient.setBackgroundTintList(
                getColorStateList(R.color.primary)
            )

            binding.btnDoctor.setBackgroundTintList(
                getColorStateList(R.color.surface_container_high)
            )

            binding.btnPatient.setTextColor(getColor(R.color.yellow_cremy))
            binding.btnDoctor.setTextColor(getColor(R.color.information_text_color))

        } else {

            binding.btnDoctor.setBackgroundTintList(
                getColorStateList(R.color.primary)
            )

            binding.btnPatient.setBackgroundTintList(
                getColorStateList(R.color.surface_container_high)
            )
            binding.btnDoctor.setTextColor(getColor(R.color.yellow_cremy))
            binding.btnPatient.setTextColor(getColor(R.color.information_text_color))
        }
    }


    private fun signInWithGoogle() {
        binding.btnGoogle.isEnabled = false
        googleSignInClient.signOut().addOnCompleteListener {
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        binding.btnSignin.isEnabled = false

        val credential = GoogleAuthProvider.getCredential(idToken, null)

        FirebaseManager.auth.signInWithCredential(credential)
            .addOnSuccessListener { authResult ->
                val firebaseUser = authResult.user
                if (firebaseUser != null) {
                    val isNewUser = authResult.additionalUserInfo?.isNewUser ?: false

                    if (isNewUser) {
                        pedirCedulaUsuarioNuevo(firebaseUser)
                    } else {
                        navegarAlHome(true)
                    }
                }
            }
            .addOnFailureListener { exception ->
                binding.btnSignin.isEnabled = true
                showMessage("Autenticación fallida: ${exception.localizedMessage}")
            }
    }

    private fun pedirCedulaUsuarioNuevo(firebaseUser: com.google.firebase.auth.FirebaseUser) {
        val inputEditText = TextInputEditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            hint = "Ej: 1712345678"
            filters = arrayOf(InputFilter.LengthFilter(10))
            setTextColor(getColor(R.color.white))
        }

        val textInputLayout = TextInputLayout(this).apply {
            setPadding(45, 20, 45, 0)
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_FILLED
            boxBackgroundColor = getColor(R.color.surface_container_high)

            setBoxCornerRadii(12f, 12f, 12f, 12f)

            addView(inputEditText)
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Paso Final Obligatorio")
            .setMessage("Para completar tu registro en MAP SALUD, por favor ingresa tu número de cédula:")
            .setView(textInputLayout)
            .setCancelable(false)
            .setPositiveButton("Finalizar Registro", null)
            .setNegativeButton("Cancelar") { d, _ ->
                FirebaseManager.auth.signOut()
                binding.btnSignin.isEnabled = true
                d.dismiss()
            }
            .create()

        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val cedula = inputEditText.text.toString().trim()

            if (!cedula.matches(Regex("\\d{10}"))) {
                textInputLayout.error = "La cédula debe tener exactamente 10 dígitos"
                return@setOnClickListener
            }

            textInputLayout.error = null
            dialog.dismiss()

            showMessage("Guardando perfil médico...")

            val userData = hashMapOf(
                "uid" to firebaseUser.uid,
                "name" to (firebaseUser.displayName ?: "Usuario Google"),
                "idNumber" to cedula,
                "email" to (firebaseUser.email ?: "")
            )

            FirebaseManager.db.collection("users")
                .document(firebaseUser.uid)
                .set(userData)
                .addOnSuccessListener {
                    navegarAlHome(true)
                }
                .addOnFailureListener {
                    binding.btnSignin.isEnabled = true
                    FirebaseManager.auth.signOut()
                    showMessage("Error al crear perfil: ${it.localizedMessage}")
                }
        }
    }

    private fun navegarAlHome(user: Boolean) {
        showMessage("Bienvenido")

        val intent = if (user) {
            Intent(this, PrincipalUser::class.java)
        } else {
            Intent(this, PrincipalMedic::class.java)
        }

        intent.putExtra("xx1", "Bienvenido de nuevo")
        startActivity(intent)
        finish()
    }


    private fun showMessage(message: String) {
        val snackbar = Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
        snackbar.setBackgroundTint(getColor(R.color.black_soft))
        snackbar.setTextColor(getColor(R.color.white))
        snackbar.show()
    }


    private fun sendSecurityAlert(email: String) {

        val request = EmailJSRequest(
            service_id = "service_l7uoiyl",
            template_id = "template_2qxrwor",
            user_id = "cC9mD7QTe_bQAMAZQ",
            accessToken = "64bpTMQJNY6--yhc3HQtK",
            template_params = mapOf(
                "user_name" to "Usuario",
                "email" to email,
                "date" to SimpleDateFormat(
                    "dd/MM/yyyy",
                    Locale.getDefault()
                ).format(Date()),
                "time" to SimpleDateFormat(
                    "HH:mm",
                    Locale.getDefault()
                ).format(Date())

            )
        )

        RetrofitClient.emailService
            .sendEmail(request)
            .enqueue(
                object : retrofit2.Callback<Void> {

                    override fun onResponse(
                        call: retrofit2.Call<Void>,
                        response: retrofit2.Response<Void>
                    ) {
                        if (response.isSuccessful) {
                            counter = 0
                            showMessage("Email enviado correctamente")
                        }
                    }

                    override fun onFailure(
                        call: retrofit2.Call<Void>,
                        t: Throwable
                    ) {
                        showMessage("Novali")
                    }
                }
            )
    }
}