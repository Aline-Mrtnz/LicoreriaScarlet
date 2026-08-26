package com.example.scarlet.data.model

/**
 * Representa una fila de la sección "Top Productos" en la pantalla de Reportes.
 */
data class TopProducto(
    val nombre: String,
    val categoria: String,
    val precio: Double,
    val vendidos: Int,
    val imagenResId: Int
)
