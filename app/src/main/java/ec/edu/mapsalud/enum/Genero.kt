package ec.edu.mapsalud.enum

enum class Genero(val valor: String) {
    MASCULINO("Masculino"),
    FEMENINO("Femenino"),
    OTRO("Otro"),
    NO_ESPECIFICADO("No especificado");

    companion object {
        fun fromString(valor: String): Genero? {
            return values().find { it.valor == valor }
        }
    }
}