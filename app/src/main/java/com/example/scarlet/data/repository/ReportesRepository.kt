package com.example.scarlet.data.repository

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.example.scarlet.data.model.Reportes
import com.example.scarlet.database.databasehelpers

class ReportesRepository(private val context: Context) {

    private val dbHelper = databasehelpers(context)

    fun crear(reporte: Reportes): Long {
        val db = dbHelper.writableDatabase
        val valores = ContentValues().apply {
            put("tipo_reporte", reporte.tipo_reporte)
            put("descripcion", reporte.descripcion)
            put("fecha_generacion", reporte.fecha_generacion)
            put("cuenta_id_cuenta", reporte.cuenta_id_cuenta)
        }
        val id = db.insert("reportes", null, valores)
        db.close()
        return id
    }

    fun editar(reporte: Reportes): Int {
        val db = dbHelper.writableDatabase
        val valores = ContentValues().apply {
            put("tipo_reporte", reporte.tipo_reporte)
            put("descripcion", reporte.descripcion)
            put("fecha_generacion", reporte.fecha_generacion)
            put("cuenta_id_cuenta", reporte.cuenta_id_cuenta)
        }
        val filas = db.update("reportes", valores, "id_reporte = ?", arrayOf(reporte.id_reporte.toString()))
        db.close()
        return filas
    }

    fun eliminar(id_reporte: Int): Int {
        val db = dbHelper.writableDatabase
        val filas = db.delete("reportes", "id_reporte = ?", arrayOf(id_reporte.toString()))
        db.close()
        return filas
    }

    fun obtenerPorId(id_reporte: Int): Reportes? {
        val db = dbHelper.readableDatabase
        val cursor = db.query("reportes", null, "id_reporte = ?", arrayOf(id_reporte.toString()), null, null, null)
        var reporte: Reportes? = null
        if (cursor.moveToFirst()) reporte = mapear(cursor)
        cursor.close()
        db.close()
        return reporte
    }

    fun listar(): List<Reportes> {
        val lista = mutableListOf<Reportes>()
        val db = dbHelper.readableDatabase
        val cursor = db.query("reportes", null, null, null, null, null, "fecha_generacion DESC")
        if (cursor.moveToFirst()) {
            do { lista.add(mapear(cursor)) } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return lista
    }

    private fun mapear(cursor: Cursor): Reportes = Reportes(
        id_reporte = cursor.getInt(cursor.getColumnIndexOrThrow("id_reporte")),
        tipo_reporte = cursor.getString(cursor.getColumnIndexOrThrow("tipo_reporte")),
        descripcion = cursor.getString(cursor.getColumnIndexOrThrow("descripcion")),
        fecha_generacion = cursor.getString(cursor.getColumnIndexOrThrow("fecha_generacion")),
        cuenta_id_cuenta = cursor.getInt(cursor.getColumnIndexOrThrow("cuenta_id_cuenta"))
    )

    /**
     * Productos más vendidos (por cantidad) entre dos fechas, listos para
     * la sección "Top Productos" de la pantalla de Reportes.
     */
    fun topProductos(desde: String, hasta: String, limite: Int = 5): List<com.example.scarlet.data.model.TopProducto> {
        val lista = mutableListOf<com.example.scarlet.data.model.TopProducto>()
        val db = dbHelper.readableDatabase
        val query = """
            SELECT p.nombre_producto, p.precio_venta, p.imagen,
                   COALESCE(c.nombre_categoria, 'Sin categoría') AS nombre_categoria,
                   SUM(d.cantidad) AS total_vendidos
            FROM detalle_venta d
            INNER JOIN ventas v ON d.id_venta = v.id_venta
            INNER JOIN productos p ON d.id_producto = p.id_producto
            LEFT JOIN categorias c ON p.id_categoria = c.id_categoria
            WHERE v.fecha_venta BETWEEN ? AND ?
            GROUP BY p.id_producto
            ORDER BY total_vendidos DESC
            LIMIT ?
        """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(desde, hasta, limite.toString()))
        if (cursor.moveToFirst()) {
            do {
                val nombreImagen = cursor.getString(cursor.getColumnIndexOrThrow("imagen"))
                val resId = com.example.scarlet.util.ImagenUtils.resolver(context, nombreImagen)
                lista.add(
                    com.example.scarlet.data.model.TopProducto(
                        nombre = cursor.getString(cursor.getColumnIndexOrThrow("nombre_producto")),
                        categoria = cursor.getString(cursor.getColumnIndexOrThrow("nombre_categoria")),
                        precio = cursor.getDouble(cursor.getColumnIndexOrThrow("precio_venta")),
                        vendidos = cursor.getInt(cursor.getColumnIndexOrThrow("total_vendidos")),
                        imagenResId = resId
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return lista
    }
}
