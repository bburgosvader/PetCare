package org.example

import java.time.LocalDateTime

class Exotico(
    codigoAtencion: String,
    nombre: String,
    especie: String,
    fechaHoraIngreso: LocalDateTime,
    tipoDueno: TipoDueno,
    val esSilvestre: Boolean
) : Paciente(codigoAtencion, nombre, especie, fechaHoraIngreso, tipoDueno) {
    override val tipo = "Exótico"

    override fun calcularCosto(tiempoMinutos: Double): Double {
        val costo = tiempoMinutos / 60.0 * 20_000
        return if (esSilvestre) costo * 1.3 else costo
    }

    override fun detalle(): String = "${super.detalle()} | Silvestre: ${if (esSilvestre) "Sí" else "No"}"
}
