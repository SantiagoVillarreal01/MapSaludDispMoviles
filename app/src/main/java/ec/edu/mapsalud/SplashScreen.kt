package ec.edu.mapsalud

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import ec.edu.mapsalud.datos.FirebaseManager
import ec.edu.mapsalud.enum.Type
import ec.edu.mapsalud.medicPages.PrincipalMedic
import ec.edu.mapsalud.userPages.PrincipalUser


class SplashScreen : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        Handler(Looper.getMainLooper()).postDelayed({
            evaluarSesionDeUsuario()
        }, 2500)
    }

    private fun evaluarSesionDeUsuario() {
        val usuarioActual = FirebaseManager.auth.currentUser

        if (usuarioActual == null) {
            irAlLogin()
        } else {
            usuarioActual.reload().addOnSuccessListener {
                if (usuarioActual.isEmailVerified) {
                    buscarRolEIniciarHome(usuarioActual.uid)
                } else {
                    FirebaseManager.auth.signOut()
                    irAlLogin()
                }
            }.addOnFailureListener {
                irAlLogin()
            }
        }
    }

    private fun buscarRolEIniciarHome(uid: String) {
        FirebaseManager.db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val infoMap = document.get("info") as? Map<*, *>
                    val tipoUsuarioStr = infoMap?.get("tipoUsuario")?.toString() ?: ""

                    // Redirección directa según el Enum unificado
                    val intent = if (tipoUsuarioStr == Type.DOCTOR.name) {
                        Intent(this, PrincipalMedic::class.java)
                    } else {
                        Intent(this, PrincipalUser::class.java)
                    }

                    intent.putExtra("xx1", "Bienvenido de nuevo")
                    startActivity(intent)
                    finish()
                } else {
                    irAlLogin()
                }
            }
            .addOnFailureListener {
                irAlLogin()
            }
    }

    private fun irAlLogin() {
        startActivity(Intent(this, LoginScreen::class.java))
        finish()
    }
}