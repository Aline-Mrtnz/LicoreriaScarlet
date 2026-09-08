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
     *
     * [idCuenta] filtra solo los productos vendidos por esa cuenta (cajero
     * o admin). Null trae el top de TODAS las cuentas (vista general).
     */
    fun topProductos(desde: String, hasta: String, limite: Int = 5, idCuenta: Int? = null): List<com.example.scarlet.data.model.TopProducto> {
        val lista = mutableListOf<com.example.scarlet.data.model.TopProducto>()
        val db = dbHelper.readableDatabase
        val filtroCuenta = if (idCuenta != null) " AND v.cuenta_id_cuenta = ?" else ""
        val query = """
            SELECT p.nombre_producto, p.precio_venta, p.imagen,
                   COALESCE(c.nombre_categoria, 'Sin categoría') AS nombre_categoria,
                   SUM(d.cantidad) AS total_vendidos
            FROM detalle_venta d
            INNER JOIN ventas v ON d.id_venta = v.id_venta
            INNER JOIN productos p ON d.id_producto = p.id_producto
            LEFT JOIN categorias c ON p.id_categoria = c.id_categoria
            WHERE v.fecha_venta BETWEEN ? AND ?$filtroCuenta
            GROUP BY p.id_producto
            ORDER BY total_vendidos DESC
            LIMIT ?
        """.trimIndent()

        val args = if (idCuenta != null) arrayOf(desde, hasta, idCuenta.toString(), limite.toString())
        else arrayOf(desde, hasta, limite.toString())
        val cursor = db.rawQuery(query, args)
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

    /** Monto vendido (revenue) por categoría entre dos fechas, ordenado de mayor a menor. */
    data class CategoriaMonto(val nombre: String, val monto: Double)

    fun splitPorCategoria(desde: String, hasta: String, idCuenta: Int? = null): List<CategoriaMonto> {
        val lista = mutableListOf<CategoriaMonto>()
        val db = dbHelper.readableDatabase
        val filtroCuenta = if (idCuenta != null) " AND v.cuenta_id_cuenta = ?" else ""
        val query = """
            SELECT COALESCE(c.nombre_categoria, 'Sin categoría') AS nombre_categoria,
                   SUM(d.cantidad * d.precio_unitario) AS monto
            FROM detalle_venta d
            INNER JOIN ventas v ON d.id_venta = v.id_venta
            INNER JOIN productos p ON d.id_producto = p.id_producto
            LEFT JOIN categorias c ON p.id_categoria = c.id_categoria
            WHERE v.fecha_venta BETWEEN ? AND ?$filtroCuenta
            GROUP BY nombre_categoria
            ORDER BY monto DESC
        """.trimIndent()
        val args = if (idCuenta != null) arrayOf(desde, hasta, idCuenta.toString()) else arrayOf(desde, hasta)
        val cursor = db.rawQuery(query, args)
        if (cursor.moveToFirst()) {
            do {
                lista.add(
                    CategoriaMonto(
                        nombre = cursor.getString(cursor.getColumnIndexOrThrow("nombre_categoria")),
                        monto = cursor.getDouble(cursor.getColumnIndexOrThrow("monto"))
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return lista
    }

    /**
     * Clientes "nuevos" en el periodo: aquellos cuya PRIMERA compra (dentro
     * del alcance filtrado por [idCuenta], o en toda la tienda si es null)
     * cae dentro de [desde, hasta]. Reemplaza el "48" fijo que había en el
     * XML por un número real calculado de la base de datos.
     */
    fun clientesNuevosEntreFechas(desde: String, hasta: String, idCuenta: Int? = null): Int {
        val db = dbHelper.readableDatabase
        val filtroCuenta = if (idCuenta != null) "WHERE cuenta_id_cuenta = ?" else ""
        val query = """
            SELECT COUNT(*) FROM (
                SELECT id_cliente, MIN(fecha_venta) AS primera_compra
                FROM ventas
                $filtroCuenta
                GROUP BY id_cliente
            ) t
            WHERE t.primera_compra BETWEEN ? AND ?
        """.trimIndent()
        val args = if (idCuenta != null) arrayOf(idCuenta.toString(), desde, hasta) else arrayOf(desde, hasta)
        val cursor = db.rawQuery(query, args)
        var cantidad = 0
        if (cursor.moveToFirst()) cantidad = cursor.getInt(0)
        cursor.close()
        db.close()
        return cantidad
    }
}