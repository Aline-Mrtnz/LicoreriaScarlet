package com.example.scarlet.data.repository

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.example.scarlet.data.model.Persona
import com.example.scarlet.database.databasehelpers

class PersonaRepository(context: Context) {

    private val dbHelper = databasehelpers(context)

    fun crear(persona: Persona): Long {
        val db = dbHelper.writableDatabase
        val valores = ContentValues().apply {
            put("nombres", persona.nombres)
            put("apellidos", persona.apellidos)
            put("ci", persona.ci)
            put("telefono", persona.telefono)
        }
        val id = db.insert("persona", null, valores)
        db.close()
        return id
    }

    fun editar(persona: Persona): Int {
        val db = dbHelper.writableDatabase
        val valores = ContentValues().apply {
            put("nombres", persona.nombres)
            put("apellidos", persona.apellidos)
            put("ci", persona.ci)
            put("telefono", persona.telefono)
        }
        val filas = db.update("persona", valores, "id_persona = ?", arrayOf(persona.id_persona.toString()))
        db.close()
        return filas
    }

    fun eliminar(id_persona: Int): Int {
        val db = dbHelper.writableDatabase
        val filas = db.delete("persona", "id_persona = ?", arrayOf(id_persona.toString()))
        db.close()
        return filas
    }

    fun obtenerPorId(id_persona: Int): Persona? {
        val db = dbHelper.readableDatabase
        val cursor = db.query("persona", null, "id_persona = ?", arrayOf(id_persona.toString()), null, null, null)
        var persona: Persona? = null
        if (cursor.moveToFirst()) persona = mapear(cursor)
        cursor.close()
        db.close()
        return persona
    }

    fun listar(): List<Persona> {
        val lista = mutableListOf<Persona>()
        val db = dbHelper.readableDatabase
        val cursor = db.query("persona", null, null, null, null, null, "nombres ASC")
        if (cursor.moveToFirst()) {
            do { lista.add(mapear(cursor)) } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return lista
    }

    private fun mapear(cursor: Cursor): Persona = Persona(
        id_persona = cursor.getInt(cursor.getColumnIndexOrThrow("id_persona")),
        nombres = cursor.getString(cursor.getColumnIndexOrThrow("nombres")),
        apellidos = cursor.getString(cursor.getColumnIndexOrThrow("apellidos")),
        ci = cursor.getString(cursor.getColumnIndexOrThrow("ci")),
        telefono = cursor.getString(cursor.getColumnIndexOrThrow("telefono"))
    )
}
