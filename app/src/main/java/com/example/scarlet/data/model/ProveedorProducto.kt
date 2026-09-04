// app/src/main/java/com/example/scarlet/data/model/ProveedorProducto.kt

package com.example.scarlet.data.model

// Representa el vínculo "este proveedor vende este producto a este precio".
// nombreProducto viene de un JOIN con la tabla productos, solo para mostrarlo
// en pantalla (no se guarda duplicado en la tabla proveedor_productos).
data class ProveedorProducto(
    val idProveedorProducto: Int = 0,
    val idProveedor: Int,
    val idProducto: Int,
    val precioPactado: Double,
    val nombreProducto: String = "",
    val unidad: String = "unidad"
)