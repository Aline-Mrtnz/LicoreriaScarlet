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

    /**
     * Carga la imagen de un producto en un ImageView, soportando dos casos:
     * 1) Una ruta absoluta de archivo (imagen subida por el usuario desde
     *    "Añadir Producto", guardada en el almacenamiento interno de la app).
     * 2) El nombre de un drawable existente en el proyecto (productos de
     *    ejemplo cargados por defecto en la base de datos).
     * Si no encuentra nada válido, cae en el drawable por defecto.
     */
    fun cargarEnImageView(context: Context, imageView: android.widget.ImageView, nombre: String?) {
        if (!nombre.isNullOrBlank()) {
            val archivo = java.io.File(nombre)
            if (archivo.exists()) {
                val bitmap = android.graphics.BitmapFactory.decodeFile(archivo.absolutePath)
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap)
                    return
                }
            }
        }
        imageView.setImageResource(resolver(context, nombre))
    }
}
