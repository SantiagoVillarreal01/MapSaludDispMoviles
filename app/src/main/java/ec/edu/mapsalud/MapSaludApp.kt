package ec.edu.mapsalud

import android.app.Application
import ec.edu.mapsalud.config.CloudinaryConfig

class MapSaludApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CloudinaryConfig.inicializar(this)
    }
}