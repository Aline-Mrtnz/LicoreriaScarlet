package com.example.scarlet.data.model

/**
 * Envuelve una categoría junto con la cantidad de productos activos que
 * la están usando. Se utiliza en la pantalla de "Gestión de Categorías"
 * y para decidir si una categoría debe mostrarse en el resto de la app
 * (solo si está ACTIVA y tiene al menos 1 producto registrado).
 */
data class CategoriaConConteo(
    val categoria: Categorias,
    val cantidadProductos: Int
) {
    val esVisibleEnCatalogo: Boolean
        get() = categoria.estaActiva && cantidadProductos > 0
}