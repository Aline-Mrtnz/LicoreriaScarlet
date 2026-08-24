package com.example.scarlet.data.repository

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.example.scarlet.data.model.Pagos
import com.example.scarlet.database.databasehelpers

class PagosRepository(context: Context) {

    private val dbHelper = databasehelpers(context)

    fun crear(pago: Pagos): Long {
        val db = dbHelper.writableDatabase
        val valores = ContentValues().apply {
            put("tipo_pago", pago.tipo_pago)
        }
        val id = db.insert("pagos", null, valores)
        db.close()
        return id
    }

    fun editar(pago: Pagos): Int {
        val db = dbHelper.writableDatabase
        val valores = ContentValues().apply {
            put("tipo_pago", pago.tipo_pago)
        }
        val filas = db.update("pagos", valores, "id_pago = ?", arrayOf(pago.id_pago.toString()))
        db.close()
        return filas
    }

    fun eliminar(id_pago: Int): Int {
        val db = dbHelper.writableDatabase
        val filas = db.delete("pagos", "id_pago = ?", arrayOf(id_pago.toString()))
        db.close()
        return filas
    }

    fun obtenerPorId(id_pago: Int): Pagos? {
        val db = dbHelper.readableDatabase
        val cursor = db.query("pagos", null, "id_pago = ?", arrayOf(id_pago.toString()), null, null, null)
        var pago: Pagos? = null
        if (cursor.moveToFirst()) pago = mapear(cursor)
        cursor.close()
        db.close()
        return pago
    }

    fun listar(): List<Pagos> {
        val lista = mutableListOf<Pagos>()
        val db = dbHelper.readableDatabase
        val cursor = db.query("pagos", null, null, null, null, null, "tipo_pago ASC")
        if (cursor.moveToFirst()) {
            do { lista.add(mapear(cursor)) } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return lista
    }

    private fun mapear(cursor: Cursor): Pagos = Pagos(
        id_pago = cursor.getInt(cursor.getColumnIndexOrThrow("id_pago")),
        tipo_pago = cursor.getString(cursor.getColumnIndexOrThrow("tipo_pago"))
    )
}
