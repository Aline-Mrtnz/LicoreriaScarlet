package com.example.scarlet.data.model

/**
 * Representa un producto dentro del carrito de compras (pantalla Shopping).
 * No proviene directamente de la base de datos: se arma a partir de lo que
 * el usuario va agregando al carrito desde Inicio/Productos, a través de
 * CartManager.
 */
data class CartItem(
    val idProducto: Int,
    val nombre: String,
    val categoria: String,
    val precioUnitario: Double,
    val imagenNombre: String?,
    var cantidad: Int = 1,
    val stockDisponible: Int = Int.MAX_VALUE
) {
    val subtotal: Double
        get() = precioUnitario * cantidad
}
