package com.example.scarlet.data.model

data class PagoCompra(
    val idPagoCompra: Int = 0,
    val fecha: String,
    val metodoPago: String,
    val monto: Double,
    val efectivoRecibido: Double? = null,
    val observacion: String? = null,
    val idCompra: Int,
    val registradoPor: String? = null
)