package com.example.scarlet.data.repository

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.example.scarlet.data.model.Cuenta
import com.example.scarlet.database.databasehelpers

class CuentaRepository(context: Context) {

    private val dbHelper = databasehelpers(context)

    fun crear(cuenta: Cuenta): Long {
        val db = dbHelper.writableDatabase
        val valores = ContentValues().apply {
            put("usuario", cuenta.usuario)
            put("clave", cuenta.clave)
            put("estado", cuenta.estado)
            put("id_persona", cuenta.id_persona)
            put("id_rol", cuenta.id_rol)
        }
        val id = db.insert("cuenta", null, valores)
        db.close()
        return id
    }

    fun editar(cuenta: Cuenta): Int {
        val db = dbHelper.writableDatabase
        val valores = ContentValues().apply {
            put("usuario", cuenta.usuario)
            put("clave", cuenta.clave)
            put("estado", cuenta.estado)
            put("id_persona", cuenta.id_persona)
            put("id_rol", cuenta.id_rol)
        }
        val filas = db.update("cuenta", valores, "id_cuenta = ?", arrayOf(cuenta.id_cuenta.toString()))
        db.close()
        return filas
    }

    fun eliminar(id_cuenta: Int): Int {
        val db = dbHelper.writableDatabase
        val filas = db.delete("cuenta", "id_cuenta = ?", arrayOf(id_cuenta.toString()))
        db.close()
        return filas
    }

    fun obtenerPorId(id_cuenta: Int): Cuenta? {
        val db = dbHelper.readableDatabase
        val cursor = db.query("cuenta", null, "id_cuenta = ?", arrayOf(id_cuenta.toString()), null, null, null)
        var cuenta: Cuenta? = null
        if (cursor.moveToFirst()) cuenta = mapear(cursor)
        cursor.close()
        db.close()
        return cuenta
    }

    fun listar(): List<Cuenta> {
        val lista = mutableListOf<Cuenta>()
        val db = dbHelper.readableDatabase
        val cursor = db.query("cuenta", null, null, null, null, null, "usuario ASC")
        if (cursor.moveToFirst()) {
            do { lista.add(mapear(cursor)) } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return lista
    }

    // Verifica si un nombre de usuario ya existe (útil al crear cuentas)
    fun existeUsuario(usuario: String): Boolean {
        val db = dbHelper.readableDatabase
        val cursor = db.query("cuenta", arrayOf("id_cuenta"), "usuario = ?", arrayOf(usuario), null, null, null)
        val existe = cursor.count > 0
        cursor.close()
        db.close()
        return existe
    }

    private fun mapear(cursor: Cursor): Cuenta = Cuenta(
        id_cuenta = cursor.getInt(cursor.getColumnIndexOrThrow("id_cuenta")),
        usuario = cursor.getString(cursor.getColumnIndexOrThrow("usuario")),
        clave = cursor.getString(cursor.getColumnIndexOrThrow("clave")),
        estado = cursor.getString(cursor.getColumnIndexOrThrow("estado")),
        id_persona = cursor.getInt(cursor.getColumnIndexOrThrow("id_persona")),
        id_rol = cursor.getInt(cursor.getColumnIndexOrThrow("id_rol"))
    )
}
