package org.example

class Box(val numero: Int) {
    var estado: EstadoBox = EstadoBox.Libre
        internal set

    fun estaLibre(): Boolean = when (estado) {
        EstadoBox.Libre -> true
        is EstadoBox.EnAtencion -> false
        is EstadoBox.EnProceso -> false
        is EstadoBox.FueraDeServicio -> false
    }

    fun pacienteActual(): Paciente? = when (val actual = estado) {
        EstadoBox.Libre -> null
        is EstadoBox.EnAtencion -> actual.paciente
        is EstadoBox.EnProceso -> null
        is EstadoBox.FueraDeServicio -> null
    }

    fun detalle(): String = "Box $numero: " + when (val actual = estado) {
        EstadoBox.Libre -> "Libre"
        is EstadoBox.EnAtencion -> "EnAtencion — ${actual.paciente.detalle()}"
        is EstadoBox.EnProceso -> "EnProceso — ${actual.motivo}"
        is EstadoBox.FueraDeServicio -> "FueraDeServicio — ${actual.motivo}"
    }
}
