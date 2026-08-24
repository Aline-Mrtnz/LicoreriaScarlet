package com.example.scarlet.data.model

data class DetalleVenta(
    val id_detalle_venta: Int = 0,
    val cantidad: Int,
    val precio: Double,
    val id_venta: Int,
    val id_producto: Int
)
