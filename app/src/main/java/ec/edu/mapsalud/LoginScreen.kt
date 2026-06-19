package ec.edu.mapsalud

import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import ec.edu.mapsalud.datos.FirebaseManager
import ec.edu.mapsalud.enum.Type
import ec.edu.mapsalud.medicPages.PrincipalMedic
import ec.edu.mapsalud.userPages.PrincipalUser
import ec.edu.mapsalud.utils.EmailJSRequest
import ec.edu.mapsalud.utils.RetrofitClient
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import ec.edu.mapsalud.databinding.ActivityLoginPageBinding
import ec.edu.mapsalud.dto.Medico
import ec.edu.mapsalud.dto.Paciente
import ec.edu.mapsalud.dto.UsuarioInfo
import ec.edu.mapsalud.enum.BloodType
import ec.edu.mapsalud.enum.Specialty
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
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
            showMessage("Inicio de sesión cancelado.")
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

        FirebaseManager.auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val user = FirebaseManager.auth.currentUser
                user?.reload()?.addOnSuccessListener {
                    if (user.isEmailVerified) {
                        // SOLUCIÓN: Validamos el rol real directo desde Firestore
                        obtenerPerfilYRedirigir(user.uid)
                    } else {
                        FirebaseManager.auth.signOut()
                        showMessage("Debe verificar su correo electrónico")
                        binding.btnSignin.isEnabled = true
                    }
                }
            }
            .addOnFailureListener {
                binding.btnSignin.isEnabled = true
                counter++
                if (counter >= 3) {
                    sendSecurityAlert(email)
                }
                showMessage("Correo o contraseña incorrectos")
            }
    }

    private fun obtenerPerfilYRedirigir(uid: String) {
        FirebaseManager.db.collection("usuarios").document(uid).get()
            .addOnSuccessListener { document ->
                binding.btnSignin.isEnabled = true
                if (document != null && document.exists()) {
                    val infoMap = document.get("info") as? Map<*, *>
                    val tipoUsuarioStr = infoMap?.get("tipoUsuario")?.toString() ?: ""

                    val tipoUsuarioReal = try {
                        Type.valueOf(tipoUsuarioStr)
                    } catch (e: IllegalArgumentException) {
                        null
                    }

                    when (tipoUsuarioReal) {
                        Type.DOCTOR -> navegarAlHome(isPatient = false)
                        Type.PATIENT -> navegarAlHome(isPatient = true)
                        null -> showMessage("Error: Tipo de usuario no identificado en el servidor.")
                    }
                } else {
                    showMessage("No se encontraron datos de perfil.")
                }
            }
            .addOnFailureListener {
                binding.btnSignin.isEnabled = true
                showMessage("Error al obtener perfil: ${it.localizedMessage}")
            }
    }

    private fun updateSelection() {
        if (type == Type.PATIENT) {
            binding.btnPatient.setBackgroundTintList(getColorStateList(R.color.primary))
            binding.btnDoctor.setBackgroundTintList(getColorStateList(R.color.surface_container_high))
            binding.btnPatient.setTextColor(getColor(R.color.yellow_cremy))
            binding.btnDoctor.setTextColor(getColor(R.color.information_text_color))
        } else {
            binding.btnDoctor.setBackgroundTintList(getColorStateList(R.color.primary))
            binding.btnPatient.setBackgroundTintList(getColorStateList(R.color.surface_container_high))
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
                        // Si ya existe, leemos su rol real de la DB en vez de asumir que es paciente
                        obtenerPerfilYRedirigir(firebaseUser.uid)
                    }
                }
            }
            .addOnFailureListener { exception ->
                binding.btnSignin.isEnabled = true
                binding.btnGoogle.isEnabled = true
                showMessage("Autenticación fallida: ${exception.localizedMessage}")
            }
    }

    private fun pedirCedulaUsuarioNuevo(firebaseUser: FirebaseUser) {
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
                binding.btnGoogle.isEnabled = true
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

            val isDoctor = type == Type.DOCTOR
            showMessage(if (isDoctor) "Guardando perfil médico..." else "Guardando perfil de paciente...")

            val infoComun = UsuarioInfo(
                id = firebaseUser.uid,
                nombres = firebaseUser.displayName ?: "Usuario Google",
                apellidos = "",
                correo = firebaseUser.email ?: "",
                telefono = "",
                cedula = cedula,
                tipoUsuario = if (isDoctor) Type.DOCTOR else Type.PATIENT
            )

            val entidadAGuardar: Any = if (isDoctor) {
                Medico(
                    info = infoComun,
                    specialty = Specialty.GENERAL,
                    anosExperiencia = 0
                )
            } else {
                Paciente(
                    info = infoComun,
                    tipoSangre = BloodType.DESCONOCIDO,
                    contactoEmergencia = ""
                )
            }

            FirebaseManager.db.collection("usuarios")
                .document(firebaseUser.uid)
                .set(entidadAGuardar)
                .addOnSuccessListener {
                    binding.btnGoogle.isEnabled = true
                    navegarAlHome(isPatient = !isDoctor)
                }
                .addOnFailureListener {
                    binding.btnSignin.isEnabled = true
                    binding.btnGoogle.isEnabled = true
                    FirebaseManager.auth.signOut()
                    showMessage("Error al crear perfil: ${it.localizedMessage}")
                }
        }
    }

    private fun navegarAlHome(isPatient: Boolean) {
        showMessage("Bienvenido")
        binding.btnGoogle.isEnabled = true

        val intent = if (isPatient) {
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
            service_id = "-----",
            template_id = "-",
            user_id = "----",
            accessToken = "-----",
            template_params = mapOf(
                "user_name" to "Usuario",
                "email" to email,
                "date" to SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()),
                "time" to SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            )
        )

        RetrofitClient.emailService.sendEmail(request).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    counter = 0
                    showMessage("Email de alerta enviado.")
                }
            }
            override fun onFailure(call: Call<Void>, t: Throwable) {
                showMessage("Fallo en reporte de seguridad.")
            }
        })
    }
}