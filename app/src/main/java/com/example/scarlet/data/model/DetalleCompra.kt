package com.example.scarlet.data.model

data class DetalleCompra(
    val idDetalleCompra: Int = 0,
    val cantidad: Int,
    val precioUnitario: Double,
    val subtotal: Double,
    val idCompra: Int = 0,
    val idProducto: Int,
    val nombreProducto: String? = null
)