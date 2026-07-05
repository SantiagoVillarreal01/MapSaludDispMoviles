package ec.edu.mapsalud.config

import android.content.Context
import com.cloudinary.android.MediaManager

object CloudinaryConfig {
    fun inicializar(context: Context) {
        try {
            val config = mapOf(
                "cloud_name" to "dfnz6ipke",
                "api_key" to "124651989371714",
                "api_secret" to "ybu1Ozz5GU25JcUCIYZOpsbBMVY"
            )
            MediaManager.init(context, config)
        } catch (e: IllegalStateException) {
        }
    }
}