package com.example.scarlet.util

import android.content.Context
import com.example.scarlet.R

/**
 * Los nombres de imagen que se guardan en la base de datos (columna
 * "imagen" de productos, o el campo imagenNombre de CartItem) no siempre
 * coinciden exactamente con un drawable existente en el proyecto.
 * Esta utilidad resuelve el recurso de forma segura y, si no lo encuentra,
 * cae en el drawable por defecto en vez de crashear la app.
 */
object ImagenUtils {

    fun resolver(context: Context, nombre: String?): Int {
        if (nombre.isNullOrBlank()) return R.drawable.default_producto

        val paquete = context.packageName
        val id = context.resources.getIdentifier(nombre, "drawable", paquete)
        return if (id != 0) id else R.drawable.default_producto
    }
}
