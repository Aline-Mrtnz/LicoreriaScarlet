// app/src/main/java/com/example/scarlet/data/model/Producto.kt

package com.example.scarlet.data.model

data class Producto(
    val idProducto: Int,
    val nombreProducto: String,
    val descripcion: String?,
    val imagen: String?,
    val precioVenta: Double,
    val precioMayor: Double?,
    val precioCompra: Double?,
    val stock: Int,
    val stockMinimo: Int,
    val estado: String,
    val idCategoria: Int,
    val marcasIdMarca: Int,
    // Campos adicionales para mostrar
    val nombreCategoria: String? = null,
    val nombreMarca: String? = null
)