package com.example.scarlet.data.repository

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.example.scarlet.data.model.DetalleVenta
import com.example.scarlet.database.databasehelpers

class DetalleVentaRepository(context: Context) {

    private val dbHelper = databasehelpers(context)

    fun crear(detalle: DetalleVenta): Long {
        val db = dbHelper.writableDatabase
        val id = crear(db, detalle)
        db.close()
        return id
    }

    /**
     * Variante que reutiliza una base de datos/transacción ya abierta.
     * La usa VentasRepository.registrarVentaCompleta() para insertar todos
     * los detalles de una venta dentro de la misma transacción.
     */
    fun crear(db: SQLiteDatabase, detalle: DetalleVenta): Long {
        val valores = ContentValues().apply {
            put("cantidad", detalle.cantidad)
            put("precio_unitario", detalle.precio)
            put("subtotal", detalle.precio * detalle.cantidad)
            put("id_venta", detalle.id_venta)
            put("id_producto", detalle.id_producto)
        }
        return db.insert("detalle_venta", null, valores)
    }

    fun editar(detalle: DetalleVenta): Int {
        val db = dbHelper.writableDatabase
        val valores = ContentValues().apply {
            put("cantidad", detalle.cantidad)
            put("precio_unitario", detalle.precio)
            put("subtotal", detalle.precio * detalle.cantidad)
            put("id_venta", detalle.id_venta)
            put("id_producto", detalle.id_producto)
        }
        val filas = db.update("detalle_venta", valores, "id_detalle_venta = ?", arrayOf(detalle.id_detalle_venta.toString()))
        db.close()
        return filas
    }

    fun eliminar(id_detalle_venta: Int): Int {
        val db = dbHelper.writableDatabase
        val filas = db.delete("detalle_venta", "id_detalle_venta = ?", arrayOf(id_detalle_venta.toString()))
        db.close()
        return filas
    }

    fun obtenerPorId(id_detalle_venta: Int): DetalleVenta? {
        val db = dbHelper.readableDatabase
        val cursor = db.query("detalle_venta", null, "id_detalle_venta = ?", arrayOf(id_detalle_venta.toString()), null, null, null)
        var detalle: DetalleVenta? = null
        if (cursor.moveToFirst()) detalle = mapear(cursor)
        cursor.close()
        db.close()
        return detalle
    }

    // Lista todos los productos vendidos dentro de una venta específica
    fun listarPorVenta(id_venta: Int): List<DetalleVenta> {
        val lista = mutableListOf<DetalleVenta>()
        val db = dbHelper.readableDatabase
        val cursor = db.query("detalle_venta", null, "id_venta = ?", arrayOf(id_venta.toString()), null, null, null)
        if (cursor.moveToFirst()) {
            do { lista.add(mapear(cursor)) } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return lista
    }

    /**
     * Lista los productos vendidos en una venta ya con el nombre del
     * producto resuelto, listos para mostrarse (ej. "3x Macallan 18 Years").
     */
    fun listarPorVentaConNombre(id_venta: Int): List<Pair<String, DetalleVenta>> {
        val lista = mutableListOf<Pair<String, DetalleVenta>>()
        val db = dbHelper.readableDatabase
        val query = """
            SELECT d.*, p.nombre_producto
            FROM detalle_venta d
            INNER JOIN productos p ON d.id_producto = p.id_producto
            WHERE d.id_venta = ?
        """.trimIndent()
        val cursor = db.rawQuery(query, arrayOf(id_venta.toString()))
        if (cursor.moveToFirst()) {
            do {
                val nombre = cursor.getString(cursor.getColumnIndexOrThrow("nombre_producto"))
                lista.add(nombre to mapear(cursor))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return lista
    }

    fun listar(): List<DetalleVenta> {
        val lista = mutableListOf<DetalleVenta>()
        val db = dbHelper.readableDatabase
        val cursor = db.query("detalle_venta", null, null, null, null, null, null)
        if (cursor.moveToFirst()) {
            do { lista.add(mapear(cursor)) } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return lista
    }

    private fun mapear(cursor: Cursor): DetalleVenta = DetalleVenta(
        id_detalle_venta = cursor.getInt(cursor.getColumnIndexOrThrow("id_detalle_venta")),
        cantidad = cursor.getInt(cursor.getColumnIndexOrThrow("cantidad")),
        precio = cursor.getDouble(cursor.getColumnIndexOrThrow("precio_unitario")),
        id_venta = cursor.getInt(cursor.getColumnIndexOrThrow("id_venta")),
        id_producto = cursor.getInt(cursor.getColumnIndexOrThrow("id_producto"))
    )
}
