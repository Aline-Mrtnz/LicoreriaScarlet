package com.example.scarlet.data.repository

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.example.scarlet.data.model.Ventas
import com.example.scarlet.database.databasehelpers

class VentasRepository(context: Context) {

    private val dbHelper = databasehelpers(context)

    fun crear(venta: Ventas): Long {
        val db = dbHelper.writableDatabase
        val valores = ContentValues().apply {
            put("fecha_venta", venta.fecha_venta)
            put("total", venta.total)
            put("id_cliente", venta.id_cliente)
            put("id_pago", venta.id_pago)
            put("cuenta_id_cuenta", venta.cuenta_id_cuenta)
        }
        val id = db.insert("ventas", null, valores)
        db.close()
        return id
    }

    fun editar(venta: Ventas): Int {
        val db = dbHelper.writableDatabase
        val valores = ContentValues().apply {
            put("fecha_venta", venta.fecha_venta)
            put("total", venta.total)
            put("id_cliente", venta.id_cliente)
            put("id_pago", venta.id_pago)
            put("cuenta_id_cuenta", venta.cuenta_id_cuenta)
        }
        val filas = db.update("ventas", valores, "id_venta = ?", arrayOf(venta.id_venta.toString()))
        db.close()
        return filas
    }

    fun eliminar(id_venta: Int): Int {
        val db = dbHelper.writableDatabase
        val filas = db.delete("ventas", "id_venta = ?", arrayOf(id_venta.toString()))
        db.close()
        return filas
    }

    fun obtenerPorId(id_venta: Int): Ventas? {
        val db = dbHelper.readableDatabase
        val cursor = db.query("ventas", null, "id_venta = ?", arrayOf(id_venta.toString()), null, null, null)
        var venta: Ventas? = null
        if (cursor.moveToFirst()) venta = mapear(cursor)
        cursor.close()
        db.close()
        return venta
    }

    fun listar(): List<Ventas> {
        val lista = mutableListOf<Ventas>()
        val db = dbHelper.readableDatabase
        val cursor = db.query("ventas", null, null, null, null, null, "fecha_venta DESC")
        if (cursor.moveToFirst()) {
            do { lista.add(mapear(cursor)) } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return lista
    }

    fun listarPorCliente(id_cliente: Int): List<Ventas> {
        val lista = mutableListOf<Ventas>()
        val db = dbHelper.readableDatabase
        val cursor = db.query("ventas", null, "id_cliente = ?", arrayOf(id_cliente.toString()), null, null, "fecha_venta DESC")
        if (cursor.moveToFirst()) {
            do { lista.add(mapear(cursor)) } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return lista
    }

    private fun mapear(cursor: Cursor): Ventas = Ventas(
        id_venta = cursor.getInt(cursor.getColumnIndexOrThrow("id_venta")),
        fecha_venta = cursor.getString(cursor.getColumnIndexOrThrow("fecha_venta")),
        total = cursor.getDouble(cursor.getColumnIndexOrThrow("total")),
        id_cliente = cursor.getInt(cursor.getColumnIndexOrThrow("id_cliente")),
        id_pago = cursor.getInt(cursor.getColumnIndexOrThrow("id_pago")),
        cuenta_id_cuenta = cursor.getInt(cursor.getColumnIndexOrThrow("cuenta_id_cuenta"))
    )
}
