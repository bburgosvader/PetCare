package org.example

import java.util.Locale

fun formatearNumero(valor: Double): String = String.format(Locale.forLanguageTag("es-CL"), "%,.2f", valor)

fun formatearMonto(monto: Double): String = "$${formatearNumero(monto)}"

fun Ticket.detalle(): String = "Ticket $numeroTicket | $tipoPaciente | $codigoAtencion | ${formatearNumero(tiempoMinutos)} min | ${formatearMonto(montoPagado)}"

fun mostrarBoxes(sistema: PetCare) {
    sistema.boxes.forEach { println(it.detalle()) }
}

fun mostrarHistorial(sistema: PetCare) {
    println("Historial del turno")
    if (sistema.historial.isEmpty()) println("Todavía no hay atenciones finalizadas.")
    sistema.historial.forEach { println(it.detalle()) }
}

fun mostrarRecaudaciones(sistema: PetCare) {
    sistema.recaudacionPorTipo.forEach { (tipo, monto) -> println("$tipo: ${formatearMonto(monto)}") }
    println("Total recaudado: ${formatearMonto(sistema.recaudacionTotal)}")
}

fun mostrarConsultas(sistema: PetCare) {
    println("1. Boxes disponibles: ${sistema.cantidadBoxesDisponibles()}")
    println("2. Pacientes finalizados con convenio:")
    val convenio = sistema.pacientesConConvenio()
    if (convenio.isEmpty()) println("Ninguno.")
    convenio.forEach { println(it.detalle()) }
    println("3. Ingreso promedio: ${formatearMonto(sistema.ingresoPromedio())}")
    println("4. Códigos finalizados: ${sistema.codigosFinalizados().joinToString().ifEmpty { "Ninguno" }}")
    println("5. Paciente con mayor tiempo: ${sistema.pacienteConMayorTiempo()?.detalle() ?: "Sin atenciones"}")
}

fun mostrarReporteCierre(sistema: PetCare) {
    println("\n${sistema.nombre} — Reporte de cierre de turno")
    mostrarHistorial(sistema)
    mostrarRecaudaciones(sistema)
    println("Cantidad de pacientes atendidos: ${sistema.historial.size}")
    println("Ingreso promedio: ${formatearMonto(sistema.ingresoPromedio())}")
    println("Tipo con más ingresos: ${sistema.tiposConMasIngresos().joinToString().ifEmpty { "Sin atenciones" }}")
    println("Boxes disponibles al cierre: ${sistema.cantidadBoxesDisponibles()}")
}
