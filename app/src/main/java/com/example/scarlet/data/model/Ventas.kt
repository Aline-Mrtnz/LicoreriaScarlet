package com.example.scarlet.data.model

data class Ventas(
    val id_venta: Int = 0,
    val fecha_venta: String,
    val total: Double,
    val id_cliente: Int,
    val id_pago: Int,
    val cuenta_id_cuenta: Int
)
