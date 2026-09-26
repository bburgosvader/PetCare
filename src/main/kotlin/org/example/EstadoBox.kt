package org.example

sealed class EstadoBox {
    data object Libre : EstadoBox()
    data class EnAtencion(val paciente: Paciente) : EstadoBox()
    data class EnProceso(val motivo: String) : EstadoBox()
    data class FueraDeServicio(val motivo: String) : EstadoBox()
}
