package com.example.scarlet.data.repository

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.example.scarlet.data.model.Categorias
import com.example.scarlet.database.databasehelpers

class CategoriasRepository(context: Context) {

    private val dbHelper = databasehelpers(context)

    fun crear(categoria: Categorias): Long {
        val db = dbHelper.writableDatabase
        val valores = ContentValues().apply {
            put("nombre_categoria", categoria.nombre_categoria)
            put("descripcion", categoria.descripcion)
        }
        val id = db.insert("categorias", null, valores)
        db.close()
        return id
    }

    fun editar(categoria: Categorias): Int {
        val db = dbHelper.writableDatabase
        val valores = ContentValues().apply {
            put("nombre_categoria", categoria.nombre_categoria)
            put("descripcion", categoria.descripcion)
        }
        val filas = db.update("categorias", valores, "id_categoria = ?", arrayOf(categoria.id_categoria.toString()))
        db.close()
        return filas
    }

    fun eliminar(id_categoria: Int): Int {
        val db = dbHelper.writableDatabase
        val filas = db.delete("categorias", "id_categoria = ?", arrayOf(id_categoria.toString()))
        db.close()
        return filas
    }

    fun obtenerPorId(id_categoria: Int): Categorias? {
        val db = dbHelper.readableDatabase
        val cursor = db.query("categorias", null, "id_categoria = ?", arrayOf(id_categoria.toString()), null, null, null)
        var categoria: Categorias? = null
        if (cursor.moveToFirst()) categoria = mapear(cursor)
        cursor.close()
        db.close()
        return categoria
    }

    fun listar(): List<Categorias> {
        val lista = mutableListOf<Categorias>()
        val db = dbHelper.readableDatabase
        val cursor = db.query("categorias", null, null, null, null, null, "nombre_categoria ASC")
        if (cursor.moveToFirst()) {
            do { lista.add(mapear(cursor)) } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return lista
    }

    private fun mapear(cursor: Cursor): Categorias = Categorias(
        id_categoria = cursor.getInt(cursor.getColumnIndexOrThrow("id_categoria")),
        nombre_categoria = cursor.getString(cursor.getColumnIndexOrThrow("nombre_categoria")),
        descripcion = cursor.getString(cursor.getColumnIndexOrThrow("descripcion"))
    )
}
