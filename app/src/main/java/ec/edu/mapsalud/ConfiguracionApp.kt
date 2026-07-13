package ec.edu.mapsalud

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import ec.edu.mapsalud.databinding.ActivityConfiguracionAppBinding
import ec.edu.mapsalud.utils.ThemeUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ConfiguracionApp : AppCompatActivity() {

    private lateinit var binding: ActivityConfiguracionAppBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConfiguracionAppBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnRegresar.setOnClickListener {
            finish()
        }

        configurarEncabezadoUsuario()
        initListeners()
    }

    private fun configurarEncabezadoUsuario() {
        // 1. Carga INSTANTÁNEA desde el Cache
        val sharedPref = getSharedPreferences("MapSaludCache", MODE_PRIVATE)
        val cachedName = sharedPref.getString("USER_NAME", "Usuario")
        val cachedExtra = sharedPref.getString("USER_EXTRA", "Cargando...")
        val cachedCedula = sharedPref.getString("USER_CEDULA", "") ?: ""

        binding.txtConfigNombreUsuario.text = cachedName
        binding.txtConfigDetalleUsuario.text = if (cachedCedula.isNotEmpty()) "$cachedExtra | C.I: $cachedCedula" else cachedExtra

        // 2. Carga en SEGUNDO PLANO desde Firestore para actualizar si hubo cambios
        val uid = auth.currentUser?.uid ?: return
        
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val document = db.collection("usuarios").document(uid).get().await()
                
                withContext(Dispatchers.Main) {
                    if (document.exists()) {
                        val infoMap = document.get("info") as? Map<*, *>
                        val nombres = infoMap?.get("nombres")?.toString() ?: "Usuario"
                        val apellidos = infoMap?.get("apellidos")?.toString() ?: ""
                        val tipo = infoMap?.get("tipoUsuario")?.toString() ?: "PACIENTE"
                        val cedula = infoMap?.get("cedula")?.toString() ?: ""
                        
                        val nombreCompleto = "$nombres $apellidos".trim()
                        var detalle = ""

                        if (tipo == "DOCTOR") {
                            detalle = (document.getString("specialty") ?: "Médico")
                        } else {
                            detalle = "Paciente Asegurado"
                        }

                        val telefono = infoMap?.get("telefono")?.toString() ?: ""
                        val genero = infoMap?.get("genero")?.toString() ?: ""
                        val correo = infoMap?.get("correo")?.toString() ?: ""

                        // Actualizar UI
                        binding.txtConfigNombreUsuario.text = nombreCompleto
                        binding.txtConfigDetalleUsuario.text = "$detalle | C.I: $cedula"

                        // Actualizar Cache para la próxima vez
                        sharedPref.edit().apply {
                            putString("USER_NAME", nombreCompleto)
                            putString("USER_NOMBRES", nombres)
                            putString("USER_APELLIDOS", apellidos)
                            putString("USER_TYPE", tipo)
                            putString("USER_CEDULA", cedula)
                            putString("USER_EXTRA", detalle)
                            putString("USER_TELEFONO", telefono)
                            putString("USER_GENERO", genero)
                            putString("USER_CORREO", correo)
                            apply()
                        }
                    }
                }
            } catch (e: Exception) {
                // Si falla Firestore, el cache ya salvó la UI
            }
        }
    }

    private fun initListeners() {
        // Modo Oscuro
        binding.switchModoOscuro.isChecked = ThemeUtils.getTheme(this) == ThemeUtils.THEME_DARK
        binding.switchModoOscuro.setOnCheckedChangeListener { _, isChecked ->
            val newTheme = if (isChecked) ThemeUtils.THEME_DARK else ThemeUtils.THEME_LIGHT
            ThemeUtils.setTheme(this, newTheme)
            recreate() // Recargar para aplicar cambios
        }

        binding.itemPreferencias.setOnClickListener {
            Toast.makeText(this, "Abriendo Preferencias", Toast.LENGTH_SHORT).show()
        }

        binding.itemNotificaciones.setOnClickListener {
            Toast.makeText(this, "Configuración de Notificaciones", Toast.LENGTH_SHORT).show()
        }

        binding.btnCerrarSesion.setOnClickListener {

            auth.signOut()

            getSharedPreferences("MapSaludPrefs", MODE_PRIVATE).edit().clear().apply()
            getSharedPreferences("MapSaludCache", MODE_PRIVATE).edit().clear().apply()

            Toast.makeText(this, "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show()

            val intent = Intent(this, LoginScreen::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

            startActivity(intent)

            finish()
        }
    }
}