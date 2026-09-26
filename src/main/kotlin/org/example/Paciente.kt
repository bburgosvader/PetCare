package org.example

import java.time.LocalDateTime

abstract class Paciente(
    val codigoAtencion: String,
    val nombre: String,
    val especie: String,
    val fechaHoraIngreso: LocalDateTime,
    val tipoDueno: TipoDueno
) {
    init {
        validarCodigo(codigoAtencion)
    }

    abstract val tipo: String

    protected abstract fun calcularCosto(tiempoMinutos: Double): Double

    fun calcularTarifa(tiempoMinutos: Double): Double {
        if (!tiempoMinutos.isFinite() || tiempoMinutos < 0) throw TarifaInvalidaException()

        val costo = calcularCosto(tiempoMinutos)
        val permiteCero = this is Felino && tiempoMinutos < 20
        if (!costo.isFinite() || costo < 0 || (costo == 0.0 && !permiteCero)) {
            throw TarifaInvalidaException()
        }

        // Primero IVA; el beneficio municipal se aplica sobre ese resultado.
        val montoConIva = costo * 1.19
        val montoFinal = if (tipoDueno == TipoDueno.MUNICIPAL) montoConIva * 0.5 else montoConIva
        if (!montoFinal.isFinite() || (montoFinal <= 0 && !permiteCero)) throw TarifaInvalidaException()
        return montoFinal
    }

    open fun detalle(): String = "$tipo | $codigoAtencion | $nombre | $especie | $tipoDueno | Ingreso: $fechaHoraIngreso"

    companion object {
        fun validarCodigo(codigo: String) {
            if (!Regex("[A-Za-z]{2}[0-9]{2}[A-Za-z]{2}").matches(codigo)) {
                throw CodigoAtencionInvalidoException()
            }
        }
    }
}

class CodigoAtencionInvalidoException : Exception("Código inválido: usa dos letras, dos dígitos y dos letras, por ejemplo CA12CD.")

class TarifaInvalidaException : Exception("Tarifa inválida: revisa el tiempo de atención. Solo un Felino con menos de 20 minutos puede pagar cero.")
