package ec.edu.mapsalud.userPages

import android.content.Intent
import android.os.Bundle
import android.widget.GridLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import ec.edu.mapsalud.databinding.CuadroComentarioBinding
import ec.edu.mapsalud.databinding.UserDatosHospitalBinding
import ec.edu.mapsalud.dto.CommentDtoRemote
import ec.edu.mapsalud.dto.Paciente
import ec.edu.mapsalud.enum.CenterType
import ec.edu.mapsalud.remote.impl.CommentRemoteImpl
import ec.edu.mapsalud.remote.impl.MedicalCenterRemoteImpl
import ec.edu.mapsalud.remote.inter.CommentRemote
import ec.edu.mapsalud.remote.inter.MedicalCenterRemote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class DatosHospital : AppCompatActivity() {

    private lateinit var binding: UserDatosHospitalBinding

    private val centerRepository: MedicalCenterRemote = MedicalCenterRemoteImpl()
    private val commentRepository: CommentRemote = CommentRemoteImpl()

    // Instancias de Firebase
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private var idCentroActual: String = ""

    private var especialidadSeleccionada: String? = null
    private var tarjetaSeleccionada: MaterialCardView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = UserDatosHospitalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        idCentroActual = intent.getStringExtra("ID_CENTRO") ?: ""
        val nombreInicial = intent.getStringExtra("NOMBRE_HOSPITAL") ?: "Cargando..."

        binding.txtNombreHospital.text = nombreInicial

        cargarInformacionDelCentro()
        cargarComentariosDeUsuarios()

        binding.btnEnviarResena.setOnClickListener {
            val textoComentario = binding.editComentario.text.toString().trim()
            val estrellas = binding.ratingBar.rating.toDouble()

            // Validamos que el usuario tenga sesión
            val uidPaciente = auth.currentUser?.uid
            if (uidPaciente == null) {
                Toast.makeText(this, "Debes iniciar sesión para comentar", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (estrellas == 0.0) {
                Toast.makeText(this, "Por favor, selecciona una calificación", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val nuevoComentario = CommentDtoRemote(
                idCenter = idCentroActual,
                idUser = uidPaciente,
                rating = estrellas,
                review = textoComentario.ifEmpty { "Sin comentario" }
            )

            lifecycleScope.launch(Dispatchers.Main) {
                val result = withContext(Dispatchers.IO) {
                    commentRepository.saveComment(nuevoComentario)
                }

                result.onSuccess { comentarioGuardado ->
                    // Agregamos el comentario a la vista asumiendo que es el usuario actual
                    agregarComentarioVista("Tú", "⭐".repeat(estrellas.toInt()), comentarioGuardado.review)

                    binding.editComentario.text.clear()
                    binding.ratingBar.rating = 0f
                    Toast.makeText(this@DatosHospital, "¡Reseña publicada!", Toast.LENGTH_SHORT).show()
                }.onFailure { error ->
                    Toast.makeText(this@DatosHospital, "Error al subir reseña: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.btnNuevaCita.setOnClickListener {
            if (especialidadSeleccionada == null) {
                Toast.makeText(this, "Por favor, selecciona una especialidad primero", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = Intent(this, Especialistas::class.java)
            intent.putExtra("ID_CENTRO", idCentroActual)
            intent.putExtra("ESPECIALIDAD_FILTRO", especialidadSeleccionada)
            startActivity(intent)
        }

        binding.btnBack.setOnClickListener { finish() }
    }

    private fun cargarInformacionDelCentro() {
        lifecycleScope.launch(Dispatchers.Main) {
            val result = withContext(Dispatchers.IO) {
                centerRepository.getCenterById(idCentroActual)
            }

            result.onSuccess { centro ->
                centro?.let {
                    binding.txtNombreHospital.text = it.name

                    val tipoCentro = try {
                        CenterType.valueOf(it.type).nombreMostrar
                    } catch (e: Exception) {
                        "Centro Médico"
                    }

                    binding.txtDireccionHospital.text = "📍 ${it.address}"

                    // Inyectamos la descripción real desde la base de datos
                    binding.txtDescripcionHospital.text = it.description.ifEmpty {
                        "No hay descripción disponible para este centro médico."
                    }

                    dibujarEspecialidades(it.specialties)
                }
            }
        }
    }

    private fun cargarComentariosDeUsuarios() {
        lifecycleScope.launch(Dispatchers.Main) {
            val result = withContext(Dispatchers.IO) {
                commentRepository.getCommentsByCenter(idCentroActual)
            }

            result.onSuccess { listaComentarios ->
                binding.layoutComentarios.removeAllViews()

                // Procesamos las búsquedas de usuarios en paralelo
                val comentariosConNombres = withContext(Dispatchers.IO) {
                    coroutineScope {
                        listaComentarios.map { comentario ->
                            async {
                                val nombre = obtenerNombreUsuario(comentario.idUser)
                                Pair(comentario, nombre)
                            }
                        }.awaitAll()
                    }
                }

                for ((comentario, nombreUsuario) in comentariosConNombres) {
                    val estrellasStr = "⭐".repeat(comentario.rating.toInt())
                    agregarComentarioVista(nombreUsuario, estrellasStr, comentario.review)
                }
            }
        }
    }

    private suspend fun obtenerNombreUsuario(uid: String): String {
        return try {
            val doc = db.collection("usuarios").document(uid).get().await()
            val paciente = doc.toObject(Paciente::class.java)
            if (paciente != null) {
                val primerNombre = paciente.info.nombres.split(" ").firstOrNull() ?: ""
                val primerApellido = paciente.info.apellidos.split(" ").firstOrNull() ?: ""
                "$primerNombre $primerApellido".trim()
            } else {
                "Usuario Anónimo"
            }
        } catch (e: Exception) {
            "Usuario Anónimo"
        }
    }

    private fun dibujarEspecialidades(especialidades: List<String>) {
        binding.gridEspecialidades.removeAllViews()

        for (especialidad in especialidades) {
            val card = MaterialCardView(this).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    height = GridLayout.LayoutParams.WRAP_CONTENT
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    setMargins(8, 8, 8, 8)
                }
                radius = 16f
                strokeWidth = 2

                setStrokeColor(android.content.res.ColorStateList.valueOf(0xFFEAEAEA.toInt()))
                setCardBackgroundColor(android.content.res.ColorStateList.valueOf(0xFFFFFFFF.toInt()))
            }

            val textView = TextView(this).apply {
                text = especialidad
                val paddingPx = (12 * resources.displayMetrics.density).toInt()
                setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
                gravity = android.view.Gravity.CENTER
                textSize = 14f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(0xFF424242.toInt()) // Color de texto por defecto
            }

            card.addView(textView)

            card.setOnClickListener {
                tarjetaSeleccionada?.apply {
                    setStrokeColor(android.content.res.ColorStateList.valueOf(0xFFEAEAEA.toInt()))
                    setCardBackgroundColor(android.content.res.ColorStateList.valueOf(0xFFFFFFFF.toInt()))
                    (getChildAt(0) as? TextView)?.setTextColor(0xFF424242.toInt())
                }

                card.setStrokeColor(android.content.res.ColorStateList.valueOf(0xFF00695C.toInt()))
                card.setCardBackgroundColor(android.content.res.ColorStateList.valueOf(0xFFE0F2F1.toInt()))
                textView.setTextColor(0xFF00695C.toInt())

                especialidadSeleccionada = especialidad
                tarjetaSeleccionada = card
            }

            binding.gridEspecialidades.addView(card)
        }
    }

    private fun agregarComentarioVista(usuario: String, estrellas: String, texto: String) {
        val itemBinding = CuadroComentarioBinding.inflate(layoutInflater, binding.layoutComentarios, false)
        itemBinding.txtUsuarioEstrellas.text = "$usuario - $estrellas"
        itemBinding.txtTextoComentario.text = texto
        binding.layoutComentarios.addView(itemBinding.root, 0)
    }
}