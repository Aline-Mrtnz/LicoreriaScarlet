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
    val marcasIdMarca: Int? = null,
    // Volumen de la botella en mililitros (ej. 750 para 750ml)
    val volumenMl: Int? = null,
    // Porcentaje de alcohol por volumen (ej. 40.0 para 40%)
    val abv: Double? = null,
    // Campos adicionales para mostrar
    val nombreCategoria: String? = null,
    val nombreMarca: String? = null
)