package ec.edu.mapsalud

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import ec.edu.mapsalud.databinding.ActivityEditarPerfilBinding
import com.google.android.material.snackbar.Snackbar
import ec.edu.mapsalud.datos.FirebaseManager
import android.widget.ArrayAdapter
import ec.edu.mapsalud.enum.Genero
import ec.edu.mapsalud.utils.ThemeUtils

class EditarPerfil : AppCompatActivity() {

    private lateinit var binding: ActivityEditarPerfilBinding
    private var usuarioUid: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeUtils.applyTheme(this)
        super.onCreate(savedInstanceState)
        binding = ActivityEditarPerfilBinding.inflate(layoutInflater)
        setContentView(binding.root)

        usuarioUid = FirebaseManager.auth.currentUser?.uid

        binding.btnRegresar.setOnClickListener {
            finish()
        }

        configurarSpinner()
        cargarDatosDesdeCache()

        if (usuarioUid != null) {
            cargarDatosDesdeFirestore()
        } else {
            mostrarMensaje("Sesión inválida. Vuelve a iniciar sesión.")
            finish()
        }

        configurarListeners()
    }

    private fun cargarDatosDesdeCache() {
        val sharedPref = getSharedPreferences("MapSaludCache", MODE_PRIVATE)
        binding.inputNombre.setText(sharedPref.getString("USER_NOMBRES", ""))
        binding.inputApellido.setText(sharedPref.getString("USER_APELLIDOS", ""))
        binding.inputCorreo.setText(sharedPref.getString("USER_CORREO", ""))
        binding.inputTelefono.setText(sharedPref.getString("USER_TELEFONO", ""))
        
        val genero = sharedPref.getString("USER_GENERO", Genero.NO_ESPECIFICADO.valor)
        binding.autoCompleteGeneroPerfil.setText(genero, false)
    }

    private fun configurarSpinner() {
        val generosVisibles = Genero.entries.map { it.valor }.toTypedArray()
        val adapterGenero = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, generosVisibles)
        binding.autoCompleteGeneroPerfil.setAdapter(adapterGenero)
    }

    private fun cargarDatosDesdeFirestore() {
        usuarioUid?.let { uid ->
            FirebaseManager.db.collection("usuarios").document(uid).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val infoMap = document.get("info") as? Map<*, *>
                        if (infoMap != null) {
                            val nombres = infoMap["nombres"]?.toString() ?: ""
                            val apellidos = infoMap["apellidos"]?.toString() ?: ""
                            val correo = infoMap["correo"]?.toString() ?: ""
                            val telefono = infoMap["telefono"]?.toString() ?: ""
                            val generoStr = infoMap["genero"]?.toString() ?: Genero.NO_ESPECIFICADO.valor

                            binding.inputNombre.setText(nombres)
                            binding.inputApellido.setText(apellidos)
                            binding.inputCorreo.setText(correo)
                            binding.inputTelefono.setText(telefono)
                            binding.autoCompleteGeneroPerfil.setText(generoStr, false)

                            // Actualizar Cache
                            getSharedPreferences("MapSaludCache", MODE_PRIVATE).edit().apply {
                                putString("USER_NOMBRES", nombres)
                                putString("USER_APELLIDOS", apellidos)
                                putString("USER_CORREO", correo)
                                putString("USER_TELEFONO", telefono)
                                putString("USER_GENERO", generoStr)
                                putString("USER_NAME", "$nombres $apellidos".trim())
                                apply()
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
            val generoSeleccionado = binding.autoCompleteGeneroPerfil.text.toString()

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
                    // Actualizar Cache local
                    getSharedPreferences("MapSaludCache", MODE_PRIVATE).edit().apply {
                        putString("USER_NOMBRES", nombre)
                        putString("USER_APELLIDOS", apellido)
                        putString("USER_TELEFONO", telefono)
                        putString("USER_GENERO", genero)
                        putString("USER_NAME", "$nombre $apellido".trim())
                        apply()
                    }
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
