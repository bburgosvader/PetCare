package org.example

import java.time.LocalDateTime
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val sistema = PetCare()
    println("${sistema.nombre} — Boxes iniciales")
    mostrarBoxes(sistema)

    // Fechas simuladas para reproducir exactamente los minutos del enunciado.
    val fechaHoraSalida = LocalDateTime.now()
    val pacientes = crearPacientesPrueba(fechaHoraSalida)

    println("\nEntradas: los sensores esperan 3 segundos por paciente")
    for (paciente in pacientes) {
        ejecutarConControlDeErrores { sistema.registrarEntrada(paciente) }
    }
    mostrarBoxes(sistema)
    demostrarCodigoInvalido(sistema)

    println("\nSalidas: los sensores esperan 6,5 segundos por paciente")
    for (paciente in pacientes) {
        ejecutarConControlDeErrores { sistema.registrarSalida(paciente.codigoAtencion, fechaHoraSalida) }
    }

    mostrarHistorial(sistema)
    mostrarRecaudaciones(sistema)
    mostrarConsultas(sistema)
    pacientes.firstOrNull()?.let { demostrarErrores(sistema, it) }
    mostrarReporteCierre(sistema)
}

suspend fun demostrarCodigoInvalido(sistema: PetCare) {
    println("\nPrueba de código inválido: 123ABC")
    ejecutarConControlDeErrores {
        val paciente = Canino("123ABC", "Prueba", "Canino", LocalDateTime.now(), TipoDueno.PARTICULAR)
        sistema.registrarEntrada(paciente)
    }
}

suspend fun demostrarErrores(sistema: PetCare, paciente: Paciente) {
    println("\nPrueba de paciente no encontrado")
    ejecutarConControlDeErrores { sistema.registrarSalida("CA00ZZ") }

    println("\nPrueba de tarifa cero no permitida")
    ejecutarConControlDeErrores { paciente.calcularTarifa(0.0) }
    println("Prueba de tiempo negativo")
    ejecutarConControlDeErrores { paciente.calcularTarifa(-1.0) }

    println("\nPrueba de sistema sin capacidad: boxes FueraDeServicio")
    // Se usa otro sistema para conservar intacto el historial de la demostración.
    val sistemaSinCapacidad = PetCare()
    for (numero in 1..10) sistemaSinCapacidad.dejarFueraDeServicio(numero, "Prueba de mantenimiento")
    mostrarBoxes(sistemaSinCapacidad)
    ejecutarConControlDeErrores { sistemaSinCapacidad.registrarEntrada(paciente) }
    sistemaSinCapacidad.habilitarBox(1)
    println("El sistema sigue operativo: ${sistemaSinCapacidad.boxes.first().detalle()}")
}
