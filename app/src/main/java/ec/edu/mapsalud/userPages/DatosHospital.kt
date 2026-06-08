package ec.edu.mapsalud.userPages

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.widget.Toast
import ec.edu.mapsalud.databinding.CuadroComentarioBinding
import ec.edu.mapsalud.databinding.UserDatosHospitalBinding


class DatosHospital : AppCompatActivity() {

    private lateinit var binding: UserDatosHospitalBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = UserDatosHospitalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        agregarComentarioVista("María F.", "⭐⭐⭐⭐⭐", "Excelente servicio en urgencias, muy rápidos.")
        agregarComentarioVista("Carlos D.", "⭐⭐⭐⭐", "Las instalaciones son de primera, aunque el parqueo es pequeño.")

        binding.btnEnviarResena.setOnClickListener {
            val comentario = binding.editComentario.text.toString()
            val estrellas = binding.ratingBar.rating.toInt()

            if (estrellas == 0) {
                Toast.makeText(this, "Por favor, selecciona una calificación", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val estrellasStr = "⭐".repeat(estrellas)

            agregarComentarioVista("Tú", estrellasStr, comentario.ifEmpty { "Sin comentario" })

            binding.editComentario.text.clear()
            binding.ratingBar.rating = 0f
            Toast.makeText(this, "¡Reseña publicada!", Toast.LENGTH_SHORT).show()
        }

        binding.btnNuevaCita.setOnClickListener {

            val intent = Intent(this, Especialistas::class.java)
            startActivity(intent)
        }

        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun agregarComentarioVista(usuario: String, estrellas: String, texto: String) {
        val itemBinding = CuadroComentarioBinding.inflate(layoutInflater, binding.layoutComentarios, false)

        itemBinding.txtUsuarioEstrellas.text = "$usuario - $estrellas"
        itemBinding.txtTextoComentario.text = texto

        binding.layoutComentarios.addView(itemBinding.root, 0)
    }
}