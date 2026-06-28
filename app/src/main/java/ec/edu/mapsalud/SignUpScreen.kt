package ec.edu.mapsalud

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import androidx.appcompat.app.AppCompatActivity
import ec.edu.mapsalud.datos.FirebaseManager
import com.google.android.material.snackbar.Snackbar
import ec.edu.mapsalud.databinding.ActivitySignUpPageBinding
import android.view.View
import android.widget.ArrayAdapter
import ec.edu.mapsalud.dto.Medico
import ec.edu.mapsalud.dto.Paciente
import ec.edu.mapsalud.dto.UsuarioInfo
import ec.edu.mapsalud.enum.BloodType
import ec.edu.mapsalud.enum.Specialty
import ec.edu.mapsalud.enum.Type
import ec.edu.mapsalud.enum.Genero
import ec.edu.mapsalud.utils.ThemeUtils
import ec.edu.mapsalud.utils.EmailJSRequest
import ec.edu.mapsalud.utils.RetrofitClient
import android.text.InputType
import android.view.Gravity
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SignUpScreen : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpPageBinding
    private var selectedType: Type = Type.PATIENT

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeUtils.applyTheme(this)
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configurarDropdowns()
        initListeners()
        updateRoleSelection()
    }

    private fun configurarDropdowns() {
        val generosVisibles = Genero.entries.map { it.valor }.toTypedArray()
        val adapterGenero = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, generosVisibles)
        binding.autoCompleteGenero.setAdapter(adapterGenero)
        binding.autoCompleteGenero.setText(Genero.NO_ESPECIFICADO.valor, false)

        val sangresVisibles = BloodType.entries.map { it.nombreMostrar }.toTypedArray()
        val adapterSangre = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, sangresVisibles)
        binding.autoCompleteTipoSangre.setAdapter(adapterSangre)
        binding.autoCompleteTipoSangre.setText(BloodType.DESCONOCIDO.nombreMostrar, false)

        val especialidadesVisibles = Specialty.entries.map { it.nombreMostrar }.toTypedArray()
        val adapterEspecialidad = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, especialidadesVisibles)
        binding.autoCompleteEspecialidad.setAdapter(adapterEspecialidad)
        binding.autoCompleteEspecialidad.setText(Specialty.GENERAL.nombreMostrar, false)
    }

    private fun initListeners() {
        binding.btnPatientSignUp.setOnClickListener {
            selectedType = Type.PATIENT
            updateRoleSelection()
        }

        binding.btnDoctorSignUp.setOnClickListener {
            selectedType = Type.DOCTOR
            updateRoleSelection()
        }

        binding.btnSignUp.setOnClickListener {
            if (!validateFields()) return@setOnClickListener
            registerUser()
        }

        binding.txtAlradyHaveAccount.setOnClickListener {
            val intent = Intent(this, LoginScreen::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun updateRoleSelection() {
        if (selectedType == Type.PATIENT) {
            binding.btnPatientSignUp.setBackgroundTintList(getColorStateList(R.color.brand_primary))
            binding.btnDoctorSignUp.setBackgroundTintList(getColorStateList(R.color.surface_container_high))
            binding.btnPatientSignUp.setTextColor(getColor(R.color.white))
            binding.btnDoctorSignUp.setTextColor(getColor(R.color.grey_medium))
            
            binding.containerPaciente.visibility = View.VISIBLE
            binding.containerMedico.visibility = View.GONE
        } else {
            binding.btnDoctorSignUp.setBackgroundTintList(getColorStateList(R.color.brand_primary))
            binding.btnPatientSignUp.setBackgroundTintList(getColorStateList(R.color.surface_container_high))
            binding.btnDoctorSignUp.setTextColor(getColor(R.color.white))
            binding.btnPatientSignUp.setTextColor(getColor(R.color.grey_medium))
            
            binding.containerPaciente.visibility = View.GONE
            binding.containerMedico.visibility = View.VISIBLE
        }
    }

    private fun registerUser() {
        val email = binding.email.text.toString().trim()
        val firstName = binding.nombres.text.toString().trim()

        binding.btnSignUp.isEnabled = false
        binding.btnSignUp.text = "Verificando..."

        // Iniciamos el proceso de verificación POR CÓDIGO primero
        solicitarVerificacionCodigo(email, firstName) {
            // Este bloque solo se ejecuta SI el código es correcto
            binding.btnSignUp.text = "Registrando..."
            
            val password = binding.password.text.toString()

            FirebaseManager.auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    val firebaseUser = result.user

                    if (firebaseUser == null) {
                        binding.btnSignUp.isEnabled = true
                        binding.btnSignUp.text = "Registrarse"
                        showMessage("Error al obtener el usuario creado")
                        return@addOnSuccessListener
                    }

                    val isDoctor = selectedType == Type.DOCTOR
                    val generoSeleccionado = binding.autoCompleteGenero.text.toString()

                    val infoComun = UsuarioInfo(
                        id = firebaseUser.uid,
                        nombres = firstName,
                        apellidos = binding.apellidos.text.toString().trim(),
                        correo = email,
                        telefono = binding.telefono.text.toString().trim(),
                        cedula = binding.idNumber.text.toString().trim(),
                        genero = generoSeleccionado,
                        tipoUsuario = selectedType
                    )

                    val entidadAGuardar: Any = if (isDoctor) {
                        val espNombre = binding.autoCompleteEspecialidad.text.toString()
                        val specialty = Specialty.entries.find { it.nombreMostrar == espNombre } ?: Specialty.GENERAL
                        
                        Medico(
                            info = infoComun,
                            specialty = specialty,
                            anosExperiencia = binding.anosExperiencia.text.toString().toIntOrNull() ?: 0
                        )
                    } else {
                        val sangreNombre = binding.autoCompleteTipoSangre.text.toString()
                        val bloodType = BloodType.entries.find { it.nombreMostrar == sangreNombre } ?: BloodType.DESCONOCIDO
                        
                        Paciente(
                            info = infoComun,
                            tipoSangre = bloodType,
                            contactoEmergencia = binding.contactoEmergencia.text.toString().trim()
                        )
                    }

                    FirebaseManager.db.collection("usuarios").document(firebaseUser.uid)
                        .set(entidadAGuardar)
                        .addOnSuccessListener {
                            binding.btnSignUp.isEnabled = true
                            binding.btnSignUp.text = "Registrarse"
                            showMessage("Registro exitoso. Bienvenido a MAP SALUD.")
                            val intent = Intent(this, LoginScreen::class.java)
                            startActivity(intent)
                            finish()
                        }
                        .addOnFailureListener {
                            binding.btnSignUp.isEnabled = true
                            binding.btnSignUp.text = "Registrarse"
                            showMessage("Error guardando datos: ${it.message}")
                        }
                }
                .addOnFailureListener { exception ->
                    binding.btnSignUp.isEnabled = true
                    binding.btnSignUp.text = "Registrarse"

                    if (exception.message?.contains("email address is already in use", true) == true) {
                        showMessage("Este correo ya está registrado")
                    } else {
                        showMessage(exception.message ?: "Error al registrar usuario")
                    }
                }
        }
    }

    private fun solicitarVerificacionCodigo(email: String, firstName: String, onVerified: () -> Unit) {
        val verificationCode = (100000..999999).random().toString()
        
        enviarEmailCodigo(email, firstName, verificationCode)

        val inputEditText = TextInputEditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            gravity = Gravity.CENTER
            textSize = 28f
            letterSpacing = 0.4f
            maxLines = 1
            filters = arrayOf(android.text.InputFilter.LengthFilter(6))
            setTextColor(getColor(R.color.brand_primary))
        }

        val textInputLayout = TextInputLayout(this).apply {
            setPadding(80, 40, 80, 20)
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            setBoxCornerRadii(16f, 16f, 16f, 16f)
            isHintEnabled = false // Usamos la propiedad pública correcta
            addView(inputEditText)
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Verifica tu correo")
            .setMessage("Hemos enviado un código de seguridad a $email. Por favor, ingrésalo para activar tu cuenta.")
            .setView(textInputLayout)
            .setCancelable(false)
            .setPositiveButton("Verificar", null)
            .setNegativeButton("Reenviar") { _, _ ->
                solicitarVerificacionCodigo(email, firstName, onVerified)
            }
            .setNeutralButton("Cancelar") { _, _ ->
                binding.btnSignUp.isEnabled = true
                binding.btnSignUp.text = "Registrarse"
            }
            .show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val input = inputEditText.text.toString().trim()
            if (input == verificationCode) {
                dialog.dismiss()
                onVerified()
            } else {
                textInputLayout.error = "Código incorrecto"
            }
        }
    }

    private fun enviarEmailCodigo(email: String, firstName: String, code: String) {
        val request = EmailJSRequest(
            service_id = "service_fsawwki",
            template_id = "template_4ss5um3",
            user_id = "69LUwDYIP5YGlftll",
            accessToken = "_Ipry8_aqmWRnf_ny78qC",
            template_params = mapOf(
                "to_name" to firstName,
                "user_email" to email,
                "message" to code
            )
        )

        RetrofitClient.emailService.sendEmail(request).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    showMessage("Código enviado a tu correo.")
                } else {
                    showMessage("Error al enviar código. Intenta de nuevo.")
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                showMessage("Error de conexión: ${t.localizedMessage}")
            }
        })
    }

    private fun validateFields(): Boolean {
        val nombres = binding.nombres.text.toString().trim()
        val apellidos = binding.apellidos.text.toString().trim()
        val cedula = binding.idNumber.text.toString().trim()
        val telefono = binding.telefono.text.toString().trim()
        val email = binding.email.text.toString().trim()
        val password = binding.password.text.toString()

        if (nombres.isEmpty()) {
            showMessage("Ingrese sus nombres")
            return false
        }
        if (apellidos.isEmpty()) {
            showMessage("Ingrese sus apellidos")
            return false
        }
        if (cedula.isEmpty() || !cedula.matches(Regex("\\d{10}"))) {
            showMessage("Ingrese una cédula válida de 10 dígitos")
            return false
        }
        if (telefono.isEmpty() || !telefono.matches(Regex("\\d{10}"))) {
            showMessage("Ingrese un número de teléfono válido")
            return false
        }
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showMessage("Correo inválido")
            return false
        }
        if (password.length < 6) {
            showMessage("La contraseña debe tener al menos 6 caracteres")
            return false
        }

        val generoSeleccionado = binding.autoCompleteGenero.text.toString()
        if (generoSeleccionado.isEmpty() || generoSeleccionado == Genero.NO_ESPECIFICADO.valor) {
            showMessage("Seleccione su género")
            return false
        }

        if (selectedType == Type.DOCTOR) {
            if (binding.autoCompleteEspecialidad.text.isEmpty()) {
                showMessage("Seleccione su especialidad")
                return false
            }
            val anos = binding.anosExperiencia.text.toString()
            if (anos.isEmpty() || anos.toIntOrNull() == null) {
                showMessage("Ingrese años de experiencia válidos")
                return false
            }
        } else {
            val sangreSeleccionada = binding.autoCompleteTipoSangre.text.toString()
            if (sangreSeleccionada.isEmpty() || sangreSeleccionada == BloodType.DESCONOCIDO.nombreMostrar) {
                showMessage("Seleccione su tipo de sangre")
                return false
            }
        }

        if (!binding.terms.isChecked) {
            showMessage("Debe aceptar los términos y condiciones")
            return false
        }

        return true
    }

    private fun showMessage(message: String) {
        val snackbar = Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
        snackbar.show()
    }
}
