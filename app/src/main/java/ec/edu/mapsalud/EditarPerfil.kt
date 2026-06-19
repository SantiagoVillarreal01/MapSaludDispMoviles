package ec.edu.mapsalud

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import ec.edu.mapsalud.databinding.ActivityEditarPerfilBinding
import com.google.android.material.snackbar.Snackbar
import ec.edu.mapsalud.datos.FirebaseManager
import android.widget.ArrayAdapter
import ec.edu.mapsalud.enum.Genero

class EditarPerfil : AppCompatActivity() {

    private lateinit var binding: ActivityEditarPerfilBinding
    private var usuarioUid: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditarPerfilBinding.inflate(layoutInflater)
        setContentView(binding.root)

        usuarioUid = FirebaseManager.auth.currentUser?.uid

        binding.btnRegresar.setOnClickListener {
            finish()
        }

        configurarSpinner()

        if (usuarioUid != null) {
            cargarDatosDesdeFirestore()
        } else {
            mostrarMensaje("Sesión inválida. Vuelve a iniciar sesión.")
            finish()
        }

        configurarListeners()
    }

    private fun configurarSpinner() {
        val generosVisibles = Genero.values().map { it.valor }.toTypedArray()
        val adapterGenero = ArrayAdapter(this, android.R.layout.simple_spinner_item, generosVisibles)
        adapterGenero.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerGeneroPerfil.adapter = adapterGenero
    }

    private fun cargarDatosDesdeFirestore() {
        usuarioUid?.let { uid ->
            FirebaseManager.db.collection("usuarios").document(uid).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val infoMap = document.get("info") as? Map<*, *>
                        if (infoMap != null) {
                            binding.inputNombre.setText(infoMap["nombres"]?.toString() ?: "")
                            binding.inputApellido.setText(infoMap["apellidos"]?.toString() ?: "")
                            binding.inputCorreo.setText(infoMap["correo"]?.toString() ?: "")
                            binding.inputTelefono.setText(infoMap["telefono"]?.toString() ?: "")

                            // Cargar Género
                            val generoStr = infoMap["genero"]?.toString() ?: Genero.NO_ESPECIFICADO.valor
                            val index = Genero.values().indexOfFirst { it.valor == generoStr }
                            if (index >= 0) {
                                binding.spinnerGeneroPerfil.setSelection(index)
                            }
                        }
                    } else {
                        mostrarMensaje("No se encontraron registros de tu perfil.")
                    }
                }
                .addOnFailureListener { e ->
                    mostrarMensaje("Error al descargar perfil: ${e.localizedMessage}")
                }
        }
    }

    private fun configurarListeners() {
        binding.btnCambiarFoto.setOnClickListener {
            Toast.makeText(this, "Funcionalidad de galería en desarrollo.", Toast.LENGTH_SHORT).show()
        }

        binding.btnGuardarCambios.setOnClickListener {
            val nombre = binding.inputNombre.text.toString().trim()
            val apellido = binding.inputApellido.text.toString().trim()
            val telefono = binding.inputTelefono.text.toString().trim()
            val generoSeleccionado = Genero.values()[binding.spinnerGeneroPerfil.selectedItemPosition].valor

            if (nombre.isEmpty() || apellido.isEmpty() || telefono.isEmpty()) {
                mostrarMensaje("Por favor, completa todos los campos obligatorios.")
                return@setOnClickListener
            }

            binding.btnGuardarCambios.isEnabled = false
            guardarCambiosEnFirestore(nombre, apellido, telefono, generoSeleccionado)
        }
    }

    private fun guardarCambiosEnFirestore(nombre: String, apellido: String, telefono: String, genero: String) {
        usuarioUid?.let { uid ->
            val actualizaciones = mapOf(
                "info.nombres" to nombre,
                "info.apellidos" to apellido,
                "info.telefono" to telefono,
                "info.genero" to genero
            )

            FirebaseManager.db.collection("usuarios").document(uid)
                .update(actualizaciones)
                .addOnSuccessListener {
                    Toast.makeText(this, "Perfil actualizado con éxito", Toast.LENGTH_LONG).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    binding.btnGuardarCambios.isEnabled = true
                    mostrarMensaje("Error al guardar cambios: ${e.localizedMessage}")
                }
        }
    }

    private fun mostrarMensaje(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).apply {
            setBackgroundTint(getColor(R.color.black_soft))
            setTextColor(getColor(R.color.white))
            show()
        }
    }
}