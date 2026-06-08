package ec.edu.mapsalud.utils

data class EmailJSRequest(
    val service_id: String,
    val template_id: String,
    val user_id: String,
    val accessToken: String, // Tu Private Key (Token de acceso)
    val template_params: Map<String, String>
)
