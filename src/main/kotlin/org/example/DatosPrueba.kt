package org.example

import java.time.LocalDateTime

fun crearPacientesPrueba(fechaHoraSalida: LocalDateTime): List<Paciente> = listOfNotNull(
    crearPacienteSeguro { Canino("CA12CD", "Max", "Golden Retriever", fechaHoraSalida.minusMinutes(75), TipoDueno.CONVENIO) },
    crearPacienteSeguro { Canino("CA99ZA", "Luna", "Labrador", fechaHoraSalida.minusMinutes(180), TipoDueno.PARTICULAR) },
    crearPacienteSeguro { Felino("FE22TO", "Misi", "Siamés", fechaHoraSalida.minusMinutes(18), TipoDueno.PARTICULAR) },
    crearPacienteSeguro { Exotico("EX44RG", "Loro", "Amazónico", fechaHoraSalida.minusMinutes(120), TipoDueno.MUNICIPAL, true) },
    crearPacienteSeguro { Exotico("EX77RG", "Iguana", "Verde", fechaHoraSalida.minusMinutes(45), TipoDueno.PARTICULAR, false) }
)

fun crearPacienteSeguro(crear: () -> Paciente): Paciente? {
    // El constructor se ejecuta dentro del try; un dato inválido no detiene los demás.
    return try {
        crear()
    } catch (error: CodigoAtencionInvalidoException) {
        println("Error: ${error.message}")
        null
    } catch (error: Exception) {
        println("No se pudo crear el paciente. Revisa sus datos.")
        null
    }
}
