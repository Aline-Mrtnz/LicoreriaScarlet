package com.example.scarlet.data.model

/**
 * Fila "aplanada" de una venta con los datos ya resueltos (cliente, pago)
 * lista para mostrarse en la pantalla de Historial de Ventas o para
 * alimentar los reportes.
 */
data class VentaResumen(
    val idVenta: Int,
    val fechaVenta: String,
    val total: Double,
    val descuento: Double,
    val nombreCliente: String,
    val tipoPago: String
)
