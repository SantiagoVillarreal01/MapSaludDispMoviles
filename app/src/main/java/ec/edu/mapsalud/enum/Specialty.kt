package ec.edu.mapsalud.enum

enum class Specialty(val nombreMostrar: String) {
    // Medicina General y Preventiva
    GENERAL("Medicina General"),
    FAMILIAR("Medicina Familiar"),
    INTERNA("Medicina Interna"),

    // Especialidades Clínicas por Sistema / Edad
    CARDIOLOGIA("Cardiología"),
    DERMATOLOGIA("Dermatología"),
    ENDOCRINOLOGIA("Endocrinología"),
    GASTROENTEROLOGIA("Gastroenterología"),
    GERIATRIA("Geriatría"),
    HEMATOLOGIA("Hematología"),
    NEFROLOGIA("Nefrología"),
    NEUMOLOGIA("Neumología"),
    NEUROLOGIA("Neurología"),
    ONCOLOGIA("Oncología"),
    PEDIATRIA("Pediatría"),
    PSIQUIATRIA("Psiquiatría"),
    REUMATOLOGIA("Reumatología"),

    // Especialidades Quirúrgicas y Médico-Quirúrgicas
    CIRUGIA_GENERAL("Cirugía General"),
    CIRUGIA_PLASTICA("Cirugía Plástica"),
    GINECOLOGIA("Ginecología y Obstetricia"),
    OFTALMOLOGIA("Oftalmología"),
    OTORRINOLARINGOLOGIA("Otorrinolaringología"),
    TRAUMATOLOGIA("Traumatología y Ortopedia"),
    UROLOGIA("Urología"),

    // Apoyo Diagnóstico y Emergencias
    ANESTESIOLOGIA("Anestesiología"),
    MINA_EMERGENCIAS("Medicina de Emergencias"),
    RADIOLOGIA("Radiología e Imagenología")
}