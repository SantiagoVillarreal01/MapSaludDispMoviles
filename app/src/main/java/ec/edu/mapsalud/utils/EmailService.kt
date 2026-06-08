package ec.edu.mapsalud.utils

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface EmailService {

    @POST("api/v1.0/email/send")
    @Headers("Content-Type: application/json")
    fun sendEmail(@Body request: EmailJSRequest): Call<Void>
}