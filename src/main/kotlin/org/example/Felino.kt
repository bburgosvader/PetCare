package org.example

import java.time.LocalDateTime

class Felino(
    codigoAtencion: String,
    nombre: String,
    especie: String,
    fechaHoraIngreso: LocalDateTime,
    tipoDueno: TipoDueno
) : Paciente(codigoAtencion, nombre, especie, fechaHoraIngreso, tipoDueno) {
    override val tipo = "Felino"

    override fun calcularCosto(tiempoMinutos: Double): Double {
        // El beneficio depende del tiempo, sin importar el tipo de dueño.
        return if (tiempoMinutos < 20) 0.0 else tiempoMinutos / 60.0 * 9_000
    }
}
