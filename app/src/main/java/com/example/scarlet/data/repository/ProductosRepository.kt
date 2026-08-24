package com.example.scarlet.data.repository

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.example.scarlet.data.model.Productos
import com.example.scarlet.database.databasehelpers

class ProductosRepository(context: Context) {

    private val dbHelper = databasehelpers(context)

    fun crear(producto: Productos): Long {
        val db = dbHelper.writableDatabase
        val valores = ContentValues().apply {
            put("nombre_producto", producto.nombre_producto)
            put("descripcion", producto.descripcion)
            put("imagen", producto.imagen)
            put("precio_venta", producto.precio_venta)
            put("precio_mayor", producto.precio_mayor)
            put("stock", producto.stock)
            put("estado", producto.estado)
            put("id_categoria", producto.id_categoria)
            put("marcas_id_marca", producto.marcas_id_marca)
        }
        val id = db.insert("productos", null, valores)
        db.close()
        return id
    }

    fun editar(producto: Productos): Int {
        val db = dbHelper.writableDatabase
        val valores = ContentValues().apply {
            put("nombre_producto", producto.nombre_producto)
            put("descripcion", producto.descripcion)
            put("imagen", producto.imagen)
            put("precio_venta", producto.precio_venta)
            put("precio_mayor", producto.precio_mayor)
            put("stock", producto.stock)
            put("estado", producto.estado)
            put("id_categoria", producto.id_categoria)
            put("marcas_id_marca", producto.marcas_id_marca)
        }
        val filas = db.update("productos", valores, "id_producto = ?", arrayOf(producto.id_producto.toString()))
        db.close()
        return filas
    }

    fun eliminar(id_producto: Int): Int {
        val db = dbHelper.writableDatabase
        val filas = db.delete("productos", "id_producto = ?", arrayOf(id_producto.toString()))
        db.close()
        return filas
    }

    // Descuenta stock, útil al confirmar una venta
    fun actualizarStock(id_producto: Int, nuevoStock: Int): Int {
        val db = dbHelper.writableDatabase
        val valores = ContentValues().apply { put("stock", nuevoStock) }
        val filas = db.update("productos", valores, "id_producto = ?", arrayOf(id_producto.toString()))
        db.close()
        return filas
    }

    fun obtenerPorId(id_producto: Int): Productos? {
        val db = dbHelper.readableDatabase
        val cursor = db.query("productos", null, "id_producto = ?", arrayOf(id_producto.toString()), null, null, null)
        var producto: Productos? = null
        if (cursor.moveToFirst()) producto = mapear(cursor)
        cursor.close()
        db.close()
        return producto
    }

    fun listar(): List<Productos> {
        val lista = mutableListOf<Productos>()
        val db = dbHelper.readableDatabase
        val cursor = db.query("productos", null, null, null, null, null, "nombre_producto ASC")
        if (cursor.moveToFirst()) {
            do { lista.add(mapear(cursor)) } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return lista
    }

    fun listarPorCategoria(id_categoria: Int): List<Productos> {
        val lista = mutableListOf<Productos>()
        val db = dbHelper.readableDatabase
        val cursor = db.query("productos", null, "id_categoria = ?", arrayOf(id_categoria.toString()), null, null, "nombre_producto ASC")
        if (cursor.moveToFirst()) {
            do { lista.add(mapear(cursor)) } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return lista
    }

    private fun mapear(cursor: Cursor): Productos = Productos(
        id_producto = cursor.getInt(cursor.getColumnIndexOrThrow("id_producto")),
        nombre_producto = cursor.getString(cursor.getColumnIndexOrThrow("nombre_producto")),
        descripcion = cursor.getString(cursor.getColumnIndexOrThrow("descripcion")),
        imagen = cursor.getString(cursor.getColumnIndexOrThrow("imagen")),
        precio_venta = cursor.getDouble(cursor.getColumnIndexOrThrow("precio_venta")),
        precio_mayor = cursor.getDouble(cursor.getColumnIndexOrThrow("precio_mayor")),
        stock = cursor.getInt(cursor.getColumnIndexOrThrow("stock")),
        estado = cursor.getString(cursor.getColumnIndexOrThrow("estado")),
        id_categoria = cursor.getInt(cursor.getColumnIndexOrThrow("id_categoria")),
        marcas_id_marca = cursor.getInt(cursor.getColumnIndexOrThrow("marcas_id_marca"))
    )
}
