package org.example

data class Ticket(
    val numeroTicket: Int,
    val paciente: Paciente,
    val tiempoMinutos: Double,
    val montoPagado: Double
) {
    val tipoPaciente: String get() = paciente.tipo
    val codigoAtencion: String get() = paciente.codigoAtencion
}
