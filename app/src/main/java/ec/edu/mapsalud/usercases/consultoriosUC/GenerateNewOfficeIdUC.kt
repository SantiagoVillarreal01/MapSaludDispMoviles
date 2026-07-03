package ec.edu.mapsalud.usercases.consultoriosUC

import ec.edu.mapsalud.remote.impl.ConsultorioRepositoryImpl

class GenerateNewOfficeIdUC(val repository: ConsultorioRepositoryImpl) {
    fun invoke(): String {
        return repository.generateNewOfficeId()
    }
}