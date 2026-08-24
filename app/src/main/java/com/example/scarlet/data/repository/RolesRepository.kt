package com.example.scarlet.data.repository

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.example.scarlet.data.model.Roles
import com.example.scarlet.database.databasehelpers

class RolesRepository(context: Context) {

    private val dbHelper = databasehelpers(context)

    fun crear(rol: Roles): Long {
        val db = dbHelper.writableDatabase
        val valores = ContentValues().apply {
            put("nombre_rol", rol.nombre_rol)
            put("descripcion", rol.descripcion)
        }
        val id = db.insert("roles", null, valores)
        db.close()
        return id
    }

    fun editar(rol: Roles): Int {
        val db = dbHelper.writableDatabase
        val valores = ContentValues().apply {
            put("nombre_rol", rol.nombre_rol)
            put("descripcion", rol.descripcion)
        }
        val filas = db.update("roles", valores, "id_rol = ?", arrayOf(rol.id_rol.toString()))
        db.close()
        return filas
    }

    fun eliminar(id_rol: Int): Int {
        val db = dbHelper.writableDatabase
        val filas = db.delete("roles", "id_rol = ?", arrayOf(id_rol.toString()))
        db.close()
        return filas
    }

    fun obtenerPorId(id_rol: Int): Roles? {
        val db = dbHelper.readableDatabase
        val cursor = db.query("roles", null, "id_rol = ?", arrayOf(id_rol.toString()), null, null, null)
        var rol: Roles? = null
        if (cursor.moveToFirst()) rol = mapear(cursor)
        cursor.close()
        db.close()
        return rol
    }

    fun listar(): List<Roles> {
        val lista = mutableListOf<Roles>()
        val db = dbHelper.readableDatabase
        val cursor = db.query("roles", null, null, null, null, null, "nombre_rol ASC")
        if (cursor.moveToFirst()) {
            do { lista.add(mapear(cursor)) } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return lista
    }

    private fun mapear(cursor: Cursor): Roles = Roles(
        id_rol = cursor.getInt(cursor.getColumnIndexOrThrow("id_rol")),
        nombre_rol = cursor.getString(cursor.getColumnIndexOrThrow("nombre_rol")),
        descripcion = cursor.getString(cursor.getColumnIndexOrThrow("descripcion"))
    )
}
