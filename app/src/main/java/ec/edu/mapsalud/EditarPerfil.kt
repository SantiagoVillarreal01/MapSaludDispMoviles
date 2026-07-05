package ec.edu.mapsalud

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import ec.edu.mapsalud.databinding.ActivityEditarPerfilBinding
import com.google.android.material.snackbar.Snackbar
import ec.edu.mapsalud.config.FirebaseManager
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import com.bumptech.glide.Glide
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import ec.edu.mapsalud.enum.Genero
import ec.edu.mapsalud.utils.ThemeUtils
import com.cloudinary.android.callback.UploadCallback

class EditarPerfil : AppCompatActivity() {

    private lateinit var binding: ActivityEditarPerfilBinding
    private var usuarioUid: String? = null

    private var imagenSeleccionadaUri: Uri? = null
    private var urlFotoActualCloudinary: String = ""

    private val seleccionarImagenLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { resultado ->
        if (resultado.resultCode == Activity.RESULT_OK) {
            val data: Intent? = resultado.data
            imagenSeleccionadaUri = data?.data
            if (imagenSeleccionadaUri != null) {
                Glide.with(this)
                    .load(imagenSeleccionadaUri)
                    .centerCrop()
                    .into(binding.imgAvatarPerfil)
            }
        }
    }

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

        urlFotoActualCloudinary = sharedPref.getString("USER_FOTO_URL", "") ?: ""
        if (urlFotoActualCloudinary.isNotEmpty()) {
            Glide.with(this)
                .load(urlFotoActualCloudinary)
                .centerCrop()
                .into(binding.imgAvatarPerfil)
        }
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
                            val fotoUrl = infoMap["imageUrl"]?.toString() ?: ""

                            binding.inputNombre.setText(nombres)
                            binding.inputApellido.setText(apellidos)
                            binding.inputCorreo.setText(correo)
                            binding.inputTelefono.setText(telefono)
                            binding.autoCompleteGeneroPerfil.setText(generoStr, false)

                            urlFotoActualCloudinary = fotoUrl
                            if (fotoUrl.isNotEmpty()) {
                                Glide.with(this)
                                    .load(fotoUrl)
                                    .centerCrop()
                                    .into(binding.imgAvatarPerfil)
                            }

                            // Actualizar Cache
                            getSharedPreferences("MapSaludCache", MODE_PRIVATE).edit().apply {
                                putString("USER_NOMBRES", nombres)
                                putString("USER_APELLIDOS", apellidos)
                                putString("USER_CORREO", correo)
                                putString("USER_TELEFONO", telefono)
                                putString("USER_GENERO", generoStr)
                                putString("USER_FOTO_URL", fotoUrl)
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
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            seleccionarImagenLauncher.launch(intent)
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

            if (imagenSeleccionadaUri != null) {
                subirFotoACloudinaryYGuardar(nombre, apellido, telefono, generoSeleccionado)
            } else {
                guardarCambiosEnFirestore(nombre, apellido, telefono, generoSeleccionado, urlFotoActualCloudinary)
            }
        }
    }

    private fun subirFotoACloudinaryYGuardar(nombre: String, apellido: String, telefono: String, genero: String) {
        imagenSeleccionadaUri?.let { uri ->
            Toast.makeText(this, "Subiendo imagen...", Toast.LENGTH_SHORT).show()

            MediaManager.get().upload(uri)
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String?) {
                    }

                    override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {
                    }

                    override fun onSuccess(requestId: String?, resultData: Map<*, *>?) {
                        val secureUrl = resultData?.get("secure_url") as? String ?: ""
                        if (secureUrl.isNotEmpty()) {
                            urlFotoActualCloudinary = secureUrl
                            guardarCambiosEnFirestore(nombre, apellido, telefono, genero, secureUrl)
                        } else {
                            binding.btnGuardarCambios.isEnabled = true
                            mostrarMensaje("Error al procesar la respuesta de la foto.")
                        }
                    }

                    override fun onError(requestId: String?, error: ErrorInfo?) {
                        binding.btnGuardarCambios.isEnabled = true
                        mostrarMensaje("Error al subir a Cloudinary: ${error?.description}")
                    }

                    override fun onReschedule(requestId: String?, error: ErrorInfo?) {
                    }
                })
                .dispatch()
        }
    }

    private fun guardarCambiosEnFirestore(nombre: String, apellido: String, telefono: String, genero: String, fotoUrl: String) {
        usuarioUid?.let { uid ->
            val actualizaciones = mapOf(
                "info.nombres" to nombre,
                "info.apellidos" to apellido,
                "info.telefono" to telefono,
                "info.genero" to genero,
                "info.imageUrl" to fotoUrl
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
                        putString("USER_FOTO_URL", fotoUrl)
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
