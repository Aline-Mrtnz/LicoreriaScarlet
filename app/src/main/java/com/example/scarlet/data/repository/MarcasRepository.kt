package com.example.scarlet.data.repository

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.example.scarlet.data.model.Marcas
import com.example.scarlet.database.databasehelpers

class MarcasRepository(context: Context) {

    private val dbHelper = databasehelpers(context)

    fun crear(marca: Marcas): Long {
        val db = dbHelper.writableDatabase
        val valores = ContentValues().apply {
            put("nombre_marca", marca.nombre_marca)
            put("descripcion_marca", marca.descripcion_marca)
        }
        val id = db.insert("marcas", null, valores)
        db.close()
        return id
    }

    fun editar(marca: Marcas): Int {
        val db = dbHelper.writableDatabase
        val valores = ContentValues().apply {
            put("nombre_marca", marca.nombre_marca)
            put("descripcion_marca", marca.descripcion_marca)
        }
        val filas = db.update("marcas", valores, "id_marca = ?", arrayOf(marca.id_marca.toString()))
        db.close()
        return filas
    }

    fun eliminar(id_marca: Int): Int {
        val db = dbHelper.writableDatabase
        val filas = db.delete("marcas", "id_marca = ?", arrayOf(id_marca.toString()))
        db.close()
        return filas
    }

    fun obtenerPorId(id_marca: Int): Marcas? {
        val db = dbHelper.readableDatabase
        val cursor = db.query("marcas", null, "id_marca = ?", arrayOf(id_marca.toString()), null, null, null)
        var marca: Marcas? = null
        if (cursor.moveToFirst()) marca = mapear(cursor)
        cursor.close()
        db.close()
        return marca
    }

    fun listar(): List<Marcas> {
        val lista = mutableListOf<Marcas>()
        val db = dbHelper.readableDatabase
        val cursor = db.query("marcas", null, null, null, null, null, "nombre_marca ASC")
        if (cursor.moveToFirst()) {
            do { lista.add(mapear(cursor)) } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return lista
    }

    private fun mapear(cursor: Cursor): Marcas = Marcas(
        id_marca = cursor.getInt(cursor.getColumnIndexOrThrow("id_marca")),
        nombre_marca = cursor.getString(cursor.getColumnIndexOrThrow("nombre_marca")),
        descripcion_marca = cursor.getString(cursor.getColumnIndexOrThrow("descripcion_marca"))
    )
}
