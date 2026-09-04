package com.example.scarlet.data.model

data class Compra(
    val idCompra: Int = 0,
    val codigo: String,
    val fechaEmision: String,
    val observacion: String? = null,
    val estado: String, // PENDIENTE, RECIBIDA, ANULADA
    val total: Double,
    val idProveedor: Int,
    val cuentaIdCuenta: Int,
    // Campos de apoyo para listas/detalle (no son columnas propias de "compras")
    val razonSocialProveedor: String? = null,
    val rfcNitProveedor: String? = null,
    val condicionPagoProveedor: String? = null,
    val totalPagado: Double = 0.0
) {
    val saldoPendiente: Double
        get() = total - totalPagado
}