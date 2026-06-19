package ec.edu.mapsalud.dto

import ec.edu.mapsalud.enum.Genero
import ec.edu.mapsalud.enum.Type
data class UsuarioInfo(
    val id: String = "",
    val nombres: String = "",
    val apellidos: String = "",
    val correo: String = "",
    val telefono: String = "",
    val cedula: String = "",
    val genero: String = Genero.NO_ESPECIFICADO.valor,
    val tipoUsuario: Type = Type.PATIENT
)