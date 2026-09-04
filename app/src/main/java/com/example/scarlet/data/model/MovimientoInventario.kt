// app/src/main/java/com/example/scarlet/data/model/MovimientoInventario.kt

package com.example.scarlet.data.model

/**
 * Representa una entrada, salida o ajuste de stock en el kárdex del producto.
 */
data class MovimientoInventario(
    val idMovimiento: Int,
    val idProducto: Int,
    val tipo: String,          // "ENTRADA" | "SALIDA" | "AJUSTE"
    val cantidad: Int,
    val stockAnterior: Int,
    val stockNuevo: Int,
    val origen: String?,       // Ej: "Compra", "Venta", "Reserva", "Ajuste manual"
    val notas: String?,
    val fecha: String,
    val usuario: String?,
    val idProveedor: Int? = null,
    val nombreProducto: String? = null,
    val nombreProveedor: String? = null
)