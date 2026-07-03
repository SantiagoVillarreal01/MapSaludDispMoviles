package ec.edu.mapsalud.remote.inter

import ec.edu.mapsalud.dto.Medico
import ec.edu.mapsalud.dto.Paciente

interface UsuariosRepository {

    suspend fun getPacienteById(idUser: String): Result<Paciente?>
    suspend fun getDoctorById(idDoctor: String): Result<Medico?>
    suspend fun getPacienteByCedula(cedula: String): Result<Paciente?>
}