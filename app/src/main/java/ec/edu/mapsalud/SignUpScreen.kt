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

class SignUpScreen : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpPageBinding
    private var selectedType: Type = Type.PATIENT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configurarSpinners()
        initListeners()
        updateRoleSelection()
    }

    private fun configurarSpinners() {
        val generosVisibles = Genero.values().map { it.valor }.toTypedArray()
        val adapterGenero = ArrayAdapter(this, android.R.layout.simple_spinner_item, generosVisibles)
        adapterGenero.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerGenero.adapter = adapterGenero
        binding.spinnerGenero.setSelection(Genero.NO_ESPECIFICADO.ordinal)

        val sangresVisibles = BloodType.values().map { it.nombreMostrar }.toTypedArray()
        val adapterSangre = ArrayAdapter(this, android.R.layout.simple_spinner_item, sangresVisibles)
        adapterSangre.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerTipoSangre.adapter = adapterSangre

        val especialidadesVisibles = Specialty.values().map { it.nombreMostrar }.toTypedArray()
        val adapterEspecialidad = ArrayAdapter(this, android.R.layout.simple_spinner_item, especialidadesVisibles)
        adapterEspecialidad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerEspecialidad.adapter = adapterEspecialidad
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
            binding.btnPatientSignUp.setBackgroundTintList(getColorStateList(R.color.primary))
            binding.btnDoctorSignUp.setBackgroundTintList(getColorStateList(R.color.surface_container_high))
            binding.btnPatientSignUp.setTextColor(getColor(R.color.white))
            binding.btnDoctorSignUp.setTextColor(getColor(R.color.information_text_color))
            
            binding.containerPaciente.visibility = View.VISIBLE
            binding.containerMedico.visibility = View.GONE
        } else {
            binding.btnDoctorSignUp.setBackgroundTintList(getColorStateList(R.color.primary))
            binding.btnPatientSignUp.setBackgroundTintList(getColorStateList(R.color.surface_container_high))
            binding.btnDoctorSignUp.setTextColor(getColor(R.color.white))
            binding.btnPatientSignUp.setTextColor(getColor(R.color.information_text_color))
            
            binding.containerPaciente.visibility = View.GONE
            binding.containerMedico.visibility = View.VISIBLE
        }
    }

    private fun registerUser() {
        val email = binding.email.text.toString().trim()
        val password = binding.password.text.toString()

        binding.btnSignUp.isEnabled = false
        binding.btnSignUp.text = "Registrando..."

        FirebaseManager.auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val firebaseUser = result.user

                if (firebaseUser == null) {
                    binding.btnSignUp.isEnabled = true
                    showMessage("Error al obtener el usuario creado")
                    return@addOnSuccessListener
                }

                val isDoctor = selectedType == Type.DOCTOR

                val generoSeleccionado = Genero.values()[binding.spinnerGenero.selectedItemPosition].valor

                val infoComun = UsuarioInfo(
                    id = firebaseUser.uid,
                    nombres = binding.nombres.text.toString().trim(),
                    apellidos = binding.apellidos.text.toString().trim(),
                    correo = email,
                    telefono = binding.telefono.text.toString().trim(),
                    cedula = binding.idNumber.text.toString().trim(),
                    genero = generoSeleccionado,
                    tipoUsuario = selectedType
                )

                val entidadAGuardar: Any = if (isDoctor) {
                    Medico(
                        info = infoComun,
                        specialty = Specialty.values()[binding.spinnerEspecialidad.selectedItemPosition],
                        anosExperiencia = binding.anosExperiencia.text.toString().toIntOrNull() ?: 0
                    )
                } else {
                    Paciente(
                        info = infoComun,
                        tipoSangre = BloodType.values()[binding.spinnerTipoSangre.selectedItemPosition],
                        contactoEmergencia = binding.contactoEmergencia.text.toString().trim()
                    )
                }

                FirebaseManager.db.collection("usuarios").document(firebaseUser.uid)
                    .set(entidadAGuardar)
                    .addOnSuccessListener {
                        binding.btnSignUp.isEnabled = true
                        binding.btnSignUp.text = "Registrarse"

                        firebaseUser.sendEmailVerification().addOnSuccessListener {
                            showMessage("Registro exitoso. Revisa tu correo para verificar tu cuenta.")
                        }
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

    private fun validateFields(): Boolean {
        val nombres = binding.nombres.text.toString().trim()
        val cedula = binding.idNumber.text.toString().trim()
        val email = binding.email.text.toString().trim()
        val password = binding.password.text.toString()

        if (nombres.isEmpty()) {
            showMessage("Ingrese sus nombres")
            return false
        }
        if (cedula.isEmpty() || !cedula.matches(Regex("\\d{10}"))) {
            showMessage("Ingrese una cédula válida de 10 dígitos")
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

        val isDoctor = selectedType == Type.DOCTOR
        if (isDoctor) {
            if (binding.anosExperiencia.text.toString().isEmpty()) {
                showMessage("Ingrese los años de experiencia")
                return false
            }
        } else {
            if (binding.contactoEmergencia.text.toString().isEmpty()) {
                showMessage("Ingrese un número de emergencia")
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
