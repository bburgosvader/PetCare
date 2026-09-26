package org.example

import java.time.LocalDateTime
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PetCareTest {
    private val salida = LocalDateTime.of(2026, 9, 26, 18, 0)
    private fun pacientes() = crearPacientesPrueba(salida)

    @Test
    fun tarifasDeLosCincoPacientes() {
        val minutos = listOf(75.0, 180.0, 18.0, 120.0, 45.0)
        val montos = listOf(14_280.0, 42_840.0, 0.0, 30_940.0, 17_850.0)
        pacientes().forEachIndexed { indice, paciente ->
            assertEquals(montos[indice], paciente.calcularTarifa(minutos[indice]), 0.000001)
        }
    }

    @Test
    fun reglasPorTipoDeDuenoYLimiteFelino() {
        for (dueno in TipoDueno.entries) {
            val felino = Felino("FE22TO", "Misi", "Siamés", salida, dueno)
            assertEquals(0.0, felino.calcularTarifa(19.999))
            assertEquals(0.0, felino.calcularTarifa(0.0))
            val esperado = if (dueno == TipoDueno.MUNICIPAL) 1785.0 else 3570.0
            assertEquals(esperado, felino.calcularTarifa(20.0), 0.000001)
        }
        val municipal = Canino("CA12CD", "Max", "Golden Retriever", salida, TipoDueno.MUNICIPAL)
        assertEquals(7140.0, municipal.calcularTarifa(60.0), 0.000001)
        val exotico = Exotico("EX77RG", "Iguana", "Verde", salida, TipoDueno.CONVENIO, false)
        assertEquals(23800.0, exotico.calcularTarifa(60.0), 0.000001)
    }

    @Test
    fun rechazoDeTiemposInvalidos() {
        for (paciente in pacientes()) {
            for (tiempo in listOf(-1.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)) {
                assertFailsWith<TarifaInvalidaException> { paciente.calcularTarifa(tiempo) }
            }
            if (paciente !is Felino) {
                assertFailsWith<TarifaInvalidaException> { paciente.calcularTarifa(0.0) }
            }
        }
    }

    @Test
    fun regexRechazaCodigosInvalidos() {
        for (codigo in listOf("123ABC", "CA1CD", "CA123CD", "CA12CDX", " CA12CD", "CA12CD\n", "C112CD", "")) {
            assertFailsWith<CodigoAtencionInvalidoException> {
                Canino(codigo, "Max", "Golden Retriever", salida, TipoDueno.CONVENIO)
            }
        }
        Paciente.validarCodigo("CA12CD")
        Paciente.validarCodigo("ca12cd")
    }

    @Test
    fun diezBoxesYConsultasSinHistorial() {
        val sistema = PetCare()
        assertEquals((1..10).toList(), sistema.boxes.map { it.numero })
        assertTrue(sistema.boxes.all { it.estado == EstadoBox.Libre })
        assertEquals(10, sistema.cantidadBoxesDisponibles())
        assertEquals(0.0, sistema.ingresoPromedio())
        assertEquals(0.0, sistema.recaudacionTotal)
        assertTrue(sistema.pacientesConConvenio().isEmpty())
        assertTrue(sistema.codigosFinalizados().isEmpty())
        assertNull(sistema.pacienteConMayorTiempo())
        assertTrue(sistema.tiposConMasIngresos().isEmpty())
        mostrarReporteCierre(sistema)
    }

    @Test
    fun estadosYEsperasExactas() = runTest {
        val sistema = PetCare()
        val paciente = pacientes().first()
        val entrada = async { sistema.registrarEntrada(paciente) }
        runCurrent()
        assertEquals(EstadoBox.EnProceso("Registrando entrada"), sistema.boxes.first().estado)
        assertEquals(9, sistema.cantidadBoxesDisponibles())
        advanceTimeBy(2999)
        assertFalse(entrada.isCompleted)
        advanceTimeBy(1)
        runCurrent()
        assertEquals(1, entrada.await())
        assertSame(paciente, assertIs<EstadoBox.EnAtencion>(sistema.boxes.first().estado).paciente)
        val retirada = async { sistema.registrarSalida(paciente.codigoAtencion, salida) }
        runCurrent()
        assertEquals(EstadoBox.EnProceso("Calculando tarifa"), sistema.boxes.first().estado)
        advanceTimeBy(6499)
        assertFalse(retirada.isCompleted)
        advanceTimeBy(1)
        runCurrent()
        assertEquals(14280.0, retirada.await().montoPagado, 0.000001)
        assertEquals(9500, currentTime)
        assertEquals(EstadoBox.Libre, sistema.boxes.first().estado)
    }

    @Test
    fun historialRecaudacionesYConsultas() = runTest {
        val sistema = PetCare()
        val pacientes = pacientes()
        pacientes.forEach { sistema.registrarEntrada(it) }
        assertEquals(5, sistema.cantidadBoxesDisponibles())
        pacientes.forEach { sistema.registrarSalida(it.codigoAtencion, salida) }
        assertEquals(listOf(1, 2, 3, 4, 5), sistema.historial.map { it.numeroTicket })
        assertEquals(listOf(75.0, 180.0, 18.0, 120.0, 45.0), sistema.historial.map { it.tiempoMinutos })
        assertEquals(105910.0, sistema.recaudacionTotal, 0.000001)
        assertEquals(57120.0, sistema.recaudacionPorTipo.getValue("Canino"), 0.000001)
        assertEquals(0.0, sistema.recaudacionPorTipo.getValue("Felino"))
        assertEquals(48790.0, sistema.recaudacionPorTipo.getValue("Exótico"), 0.000001)
        assertEquals(10, sistema.cantidadBoxesDisponibles())
        assertEquals(listOf(pacientes.first()), sistema.pacientesConConvenio())
        assertEquals(21182.0, sistema.ingresoPromedio(), 0.000001)
        assertEquals(pacientes.map { it.codigoAtencion }, sistema.codigosFinalizados())
        assertSame(pacientes[1], sistema.pacienteConMayorTiempo())
        assertEquals(listOf("Canino"), sistema.tiposConMasIngresos())
    }

    @Test
    fun salidaInvalidaConservaPacienteYPermiteReintentar() = runTest {
        val sistema = PetCare()
        val paciente = pacientes().first()
        sistema.registrarEntrada(paciente)
        assertFailsWith<TarifaInvalidaException> {
            sistema.registrarSalida(paciente.codigoAtencion, paciente.fechaHoraIngreso.minusSeconds(1))
        }
        assertSame(paciente, sistema.boxes.first().pacienteActual())
        assertTrue(sistema.historial.isEmpty())
        assertEquals(0.0, sistema.recaudacionTotal)
        assertEquals(1, sistema.registrarSalida(paciente.codigoAtencion, salida).numeroTicket)
        assertFailsWith<PacienteNoEncontradoException> { sistema.registrarSalida(paciente.codigoAtencion, salida) }
        assertEquals(1, sistema.historial.size)
    }

    @Test
    fun fueraDeServicioNoSeAsignaYSePuedeHabilitar() = runTest {
        val sistema = PetCare()
        sistema.dejarFueraDeServicio(1, "Mantenimiento")
        assertEquals(EstadoBox.FueraDeServicio("Mantenimiento"), sistema.boxes.first().estado)
        assertEquals(9, sistema.cantidadBoxesDisponibles())
        assertEquals(2, sistema.registrarEntrada(pacientes().first()))
        assertFailsWith<IllegalStateException> { sistema.dejarFueraDeServicio(2, "Mantenimiento") }
        assertFailsWith<IllegalStateException> { sistema.habilitarBox(2) }
        sistema.habilitarBox(1)
        assertEquals(EstadoBox.Libre, sistema.boxes.first().estado)
    }

    @Test
    fun sinCapacidadConBoxesOcupados() = runTest {
        val sistema = PetCare()
        for (indice in 0..9) {
            sistema.registrarEntrada(Canino("CA${indice.toString().padStart(2, '0')}CD", "Paciente", "Canino", salida, TipoDueno.PARTICULAR))
        }
        assertEquals(0, sistema.cantidadBoxesDisponibles())
        assertFailsWith<SistemaSinCapacidadException> { sistema.registrarEntrada(pacientes().first()) }
        assertEquals(10, sistema.boxes.count { it.pacienteActual() != null })
    }

    @Test
    fun sinCapacidadConBoxesFueraDeServicio() = runTest {
        val sistema = PetCare()
        for (numero in 1..10) sistema.dejarFueraDeServicio(numero, "Mantenimiento")
        assertFailsWith<SistemaSinCapacidadException> { sistema.registrarEntrada(pacientes().first()) }
        sistema.habilitarBox(1)
        assertEquals(1, sistema.registrarEntrada(pacientes().first()))
    }

    @Test
    fun cancelacionesRestauranLosEstados() = runTest {
        val sistema = PetCare()
        val paciente = pacientes().first()
        val entrada = launch { sistema.registrarEntrada(paciente) }
        runCurrent()
        entrada.cancel()
        entrada.join()
        assertEquals(10, sistema.cantidadBoxesDisponibles())
        sistema.registrarEntrada(paciente)
        val retirada = launch { sistema.registrarSalida(paciente.codigoAtencion, salida) }
        runCurrent()
        retirada.cancel()
        retirada.join()
        assertSame(paciente, sistema.boxes.first().pacienteActual())
        assertTrue(sistema.historial.isEmpty())
        assertEquals(0.0, sistema.recaudacionTotal)
    }

    @Test
    fun entradasConcurrentesNoCompartenBox() = runTest {
        val sistema = PetCare()
        val primera = async { sistema.registrarEntrada(pacientes()[0]) }
        val segunda = async { sistema.registrarEntrada(pacientes()[1]) }
        assertEquals(1, primera.await())
        assertEquals(2, segunda.await())
        assertEquals(8, sistema.cantidadBoxesDisponibles())
    }

    @Test
    fun controlDeErroresPermiteContinuar() = runTest {
        val sistema = PetCare()
        demostrarCodigoInvalido(sistema)
        demostrarErrores(sistema, pacientes().first())
        assertEquals(10, sistema.cantidadBoxesDisponibles())
        assertEquals(1, sistema.registrarEntrada(pacientes().first()))
    }

    @Test
    fun reporteContieneTicketsYResumen() = runTest {
        val sistema = PetCare()
        for (paciente in pacientes()) {
            sistema.registrarEntrada(paciente)
            sistema.registrarSalida(paciente.codigoAtencion, salida)
        }
        val salidaOriginal = System.out
        val contenido = ByteArrayOutputStream()
        try {
            System.setOut(PrintStream(contenido, true, Charsets.UTF_8))
            mostrarReporteCierre(sistema)
        } finally {
            System.setOut(salidaOriginal)
        }
        val reporte = contenido.toString(Charsets.UTF_8)
        sistema.historial.forEach { assertTrue(reporte.contains(it.detalle())) }
        assertTrue(reporte.contains("Total recaudado: ${formatearMonto(105910.0)}"))
        assertTrue(reporte.contains("Cantidad de pacientes atendidos: 5"))
        assertTrue(reporte.contains("Ingreso promedio: ${formatearMonto(21182.0)}"))
        assertTrue(reporte.contains("Tipo con más ingresos: Canino"))
        assertTrue(reporte.contains("Boxes disponibles al cierre: 10"))
        assertTrue(pacientes()[3].detalle().contains("Silvestre: Sí"))
        assertTrue(pacientes()[4].detalle().contains("Silvestre: No"))
    }

    @Test
    fun creacionInvalidaPermiteCrearEIngresarElSiguientePaciente() = runTest {
        val salidaOriginal = System.out
        val contenido = ByteArrayOutputStream()
        val creados = try {
            System.setOut(PrintStream(contenido, true, Charsets.UTF_8))
            listOfNotNull(
                crearPacienteSeguro { Canino("123ABC", "Inválido", "Canino", salida, TipoDueno.PARTICULAR) },
                crearPacienteSeguro { Felino("FE22TO", "Misi", "Siamés", salida.minusMinutes(18), TipoDueno.PARTICULAR) }
            )
        } finally {
            System.setOut(salidaOriginal)
        }
        assertEquals(listOf("FE22TO"), creados.map { it.codigoAtencion })
        val mensaje = contenido.toString(Charsets.UTF_8)
        assertTrue(mensaje.startsWith("Error: Código inválido:"))
        assertFalse(mensaje.contains("\tat "))
        val sistema = PetCare()
        assertEquals(1, sistema.registrarEntrada(creados.single()))
        assertEquals(0.0, sistema.registrarSalida("FE22TO", salida).montoPagado)
    }

    @Test
    fun cierreDistingueHistorialVacioDeAtencionesGratuitas() = runTest {
        val sistema = PetCare()
        val salidaOriginal = System.out
        val contenido = ByteArrayOutputStream()
        try {
            System.setOut(PrintStream(contenido, true, Charsets.UTF_8))
            mostrarReporteCierre(sistema)
            assertTrue(contenido.toString(Charsets.UTF_8).contains("Tipo con más ingresos: Sin atenciones"))
            assertTrue(sistema.tiposConMasIngresos().isEmpty())

            sistema.registrarEntrada(pacientes()[2])
            sistema.registrarSalida("FE22TO", salida)
            contenido.reset()
            mostrarReporteCierre(sistema)
        } finally {
            System.setOut(salidaOriginal)
        }
        assertEquals(listOf("Felino"), sistema.tiposConMasIngresos())
        assertEquals(0.0, sistema.recaudacionTotal)
        val reporte = contenido.toString(Charsets.UTF_8)
        assertTrue(reporte.contains("Tipo con más ingresos: Felino"))
        assertTrue(reporte.contains("Cantidad de pacientes atendidos: 1"))
        assertFalse(reporte.contains("Sin atenciones"))
    }
}
