package org.example

import java.time.LocalDateTime

class Canino(
    codigoAtencion: String,
    nombre: String,
    especie: String,
    fechaHoraIngreso: LocalDateTime,
    tipoDueno: TipoDueno
) : Paciente(codigoAtencion, nombre, especie, fechaHoraIngreso, tipoDueno) {
    override val tipo = "Canino"

    override fun calcularCosto(tiempoMinutos: Double): Double {
        val costo = tiempoMinutos / 60.0 * 12_000
        return if (tipoDueno == TipoDueno.CONVENIO) costo * 0.8 else costo
    }
}
