package org.example

import java.time.Duration
import java.time.LocalDateTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PetCare {
    val nombre = "PetCare"
    private val boxesInternos = MutableList(10) { indice -> Box(indice + 1) }
    private val historialInterno = mutableListOf<Ticket>()
    private val ingresosPorTipo = mutableMapOf("Canino" to 0.0, "Felino" to 0.0, "Exótico" to 0.0)
    private var siguienteTicket = 1

    // El Mutex evita que dos operaciones modifiquen el mismo box simultáneamente.
    private val mutex = Mutex()

    val boxes: List<Box> get() = boxesInternos.toList()
    val historial: List<Ticket> get() = historialInterno.toList()
    val recaudacionPorTipo: Map<String, Double> get() = ingresosPorTipo.toMap()
    var recaudacionTotal = 0.0
        private set

    suspend fun registrarEntrada(paciente: Paciente): Int = mutex.withLock {
        Paciente.validarCodigo(paciente.codigoAtencion)
        val box = boxesInternos.firstOrNull { it.estaLibre() } ?: throw SistemaSinCapacidadException()
        if (!box.estaLibre()) throw SistemaSinCapacidadException()

        box.estado = EstadoBox.EnProceso("Registrando entrada")
        println(box.detalle())
        var completada = false
        try {
            delay(3000)
            box.estado = EstadoBox.EnAtencion(paciente)
            completada = true
            println("Entrada registrada. ${box.detalle()}")
            box.numero
        } finally {
            // Una cancelación no debe dejar el box atrapado en EnProceso.
            if (!completada) box.estado = EstadoBox.Libre
        }
    }

    suspend fun registrarSalida(
        codigoAtencion: String,
        fechaHoraSalida: LocalDateTime = LocalDateTime.now()
    ): Ticket = mutex.withLock {
        Paciente.validarCodigo(codigoAtencion)
        val box = boxesInternos.firstOrNull { it.pacienteActual()?.codigoAtencion == codigoAtencion }
            ?: throw PacienteNoEncontradoException(codigoAtencion)
        val paciente = box.pacienteActual() ?: throw PacienteNoEncontradoException(codigoAtencion)
        val estadoAnterior = box.estado
        box.estado = EstadoBox.EnProceso("Calculando tarifa")
        println(box.detalle())
        var completada = false
        try {
            delay(6500)
            val duracion = Duration.between(paciente.fechaHoraIngreso, fechaHoraSalida)
            val tiempoMinutos = duracion.seconds / 60.0 + duracion.nano / 60_000_000_000.0
            val monto = paciente.calcularTarifa(tiempoMinutos)
            val ticket = Ticket(siguienteTicket, paciente, tiempoMinutos, monto)

            historialInterno.add(ticket)
            recaudacionTotal += monto
            ingresosPorTipo[paciente.tipo] = ingresosPorTipo.getValue(paciente.tipo) + monto
            siguienteTicket++
            box.estado = EstadoBox.Libre
            completada = true
            println(ticket.detalle())
            println("Salida registrada. ${box.detalle()}")
            ticket
        } finally {
            // Si falla la tarifa o se cancela la espera, conservamos al paciente.
            if (!completada) box.estado = estadoAnterior
        }
    }

    suspend fun dejarFueraDeServicio(numero: Int, motivo: String) = mutex.withLock {
        val box = boxesInternos.firstOrNull { it.numero == numero }
            ?: throw IllegalArgumentException("No existe el box $numero.")
        when (box.estado) {
            EstadoBox.Libre -> box.estado = EstadoBox.FueraDeServicio(motivo)
            is EstadoBox.EnAtencion -> throw IllegalStateException("El box $numero tiene un paciente.")
            is EstadoBox.EnProceso -> throw IllegalStateException("El box $numero está procesando una operación.")
            is EstadoBox.FueraDeServicio -> throw IllegalStateException("El box $numero ya está FueraDeServicio.")
        }
    }

    suspend fun habilitarBox(numero: Int) = mutex.withLock {
        val box = boxesInternos.firstOrNull { it.numero == numero }
            ?: throw IllegalArgumentException("No existe el box $numero.")
        when (box.estado) {
            EstadoBox.Libre -> Unit
            is EstadoBox.EnAtencion -> throw IllegalStateException("El box $numero tiene un paciente.")
            is EstadoBox.EnProceso -> throw IllegalStateException("El box $numero está procesando una operación.")
            is EstadoBox.FueraDeServicio -> box.estado = EstadoBox.Libre
        }
    }

    fun cantidadBoxesDisponibles(): Int = boxesInternos.count { it.estaLibre() }

    fun pacientesConConvenio(): List<Paciente> = historialInterno
        .filter { it.paciente.tipoDueno == TipoDueno.CONVENIO }
        .map { it.paciente }

    fun ingresoPromedio(): Double = if (historialInterno.isEmpty()) 0.0
        else historialInterno.map { it.montoPagado }.average()

    fun codigosFinalizados(): List<String> = historialInterno.map { it.codigoAtencion }

    fun pacienteConMayorTiempo(): Paciente? = historialInterno
        .maxByOrNull { it.tiempoMinutos }?.paciente

    fun tiposConMasIngresos(): List<String> {
        // Solo comparamos tipos atendidos, incluso si sus atenciones fueron gratuitas.
        val tiposAtendidos = historialInterno.map { it.tipoPaciente }.distinct()
        val mayorIngreso = tiposAtendidos.maxOfOrNull { ingresosPorTipo.getValue(it) }
            ?: return emptyList()
        return tiposAtendidos.filter { ingresosPorTipo.getValue(it) == mayorIngreso }
    }
}

class PacienteNoEncontradoException(codigo: String) : Exception("No hay un paciente en atención con el código $codigo.")

class SistemaSinCapacidadException : Exception("No hay boxes Libres disponibles.")

suspend fun ejecutarConControlDeErrores(operacion: suspend () -> Unit) {
    try {
        operacion()
    } catch (error: CancellationException) {
        // La cancelación de una corrutina no es un error de datos.
        throw error
    } catch (error: CodigoAtencionInvalidoException) {
        println("Error: ${error.message}")
    } catch (error: TarifaInvalidaException) {
        println("Error: ${error.message}")
    } catch (error: PacienteNoEncontradoException) {
        println("Error: ${error.message}")
    } catch (error: SistemaSinCapacidadException) {
        println("Error: ${error.message}")
    } catch (error: Exception) {
        println("No se pudo completar la operación. Revisa los datos e inténtalo nuevamente.")
    }
}
