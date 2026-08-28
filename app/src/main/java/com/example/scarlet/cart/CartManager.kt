package com.example.scarlet.cart

import com.example.scarlet.data.model.CartItem
import com.example.scarlet.data.model.Producto

/**
 * Carrito de compras compartido por toda la aplicación.
 *
 * Al ser un singleton (object), la misma lista de productos se ve reflejada
 * tanto en la pantalla de Inicio/Productos (donde se agregan productos)
 * como en la pantalla de Shopping (donde se revisan/editan cantidades y se
 * finaliza la compra). Esto es lo que permite que "hacer clic en un
 * producto" realmente aumente el carrito en toda la app.
 */
object CartManager {

    private val items = mutableListOf<CartItem>()
    private val listeners = mutableListOf<() -> Unit>()

    fun agregarListener(listener: () -> Unit) {
        listeners.add(listener)
    }

    fun quitarListener(listener: () -> Unit) {
        listeners.remove(listener)
    }

    private fun notificarCambio() {
        listeners.forEach { it.invoke() }
    }

    fun obtenerItems(): MutableList<CartItem> = items

    /**
     * Agrega un producto al carrito. Si ya existe, incrementa la cantidad
     * (respetando el stock disponible). Devuelve un resultado para que la
     * pantalla que llama pueda mostrar el Toast/mensaje adecuado.
     */
    fun agregarProducto(producto: Producto, cantidad: Int = 1): ResultadoAgregar {
        if (producto.stock <= 0) {
            return ResultadoAgregar.SIN_STOCK
        }

        val existente = items.find { it.idProducto == producto.idProducto }
        if (existente != null) {
            val nuevaCantidad = existente.cantidad + cantidad
            if (nuevaCantidad > producto.stock) {
                existente.cantidad = producto.stock
                notificarCambio()
                return ResultadoAgregar.STOCK_MAXIMO
            }
            existente.cantidad = nuevaCantidad
            notificarCambio()
            return ResultadoAgregar.AGREGADO
        }

        val cantidadInicial = cantidad.coerceAtMost(producto.stock)
        items.add(
            CartItem(
                idProducto = producto.idProducto,
                nombre = producto.nombreProducto,
                categoria = producto.nombreCategoria ?: "General",
                precioUnitario = producto.precioVenta,
                imagenNombre = producto.imagen,
                cantidad = cantidadInicial,
                stockDisponible = producto.stock
            )
        )
        notificarCambio()
        return ResultadoAgregar.AGREGADO
    }

    fun quitarItem(item: CartItem) {
        items.remove(item)
        notificarCambio()
    }

    fun actualizarCantidad(item: CartItem, nuevaCantidad: Int) {
        if (nuevaCantidad <= 0) {
            quitarItem(item)
            return
        }
        item.cantidad = nuevaCantidad.coerceAtMost(item.stockDisponible)
        notificarCambio()
    }

    fun limpiar() {
        items.clear()
        notificarCambio()
    }

    fun totalItems(): Int = items.sumOf { it.cantidad }

    fun subtotal(): Double = items.sumOf { it.subtotal }

    fun estaVacio(): Boolean = items.isEmpty()

    enum class ResultadoAgregar {
        AGREGADO, SIN_STOCK, STOCK_MAXIMO
    }
}
