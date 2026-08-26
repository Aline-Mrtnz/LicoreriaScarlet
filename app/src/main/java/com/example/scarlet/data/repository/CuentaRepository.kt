package com.example.scarlet.data.repository

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.example.scarlet.database.databasehelpers

data class UsuarioInfo(
    val idCuenta: Int,
    val usuario: String,
    val nombres: String,
    val apellidos: String,
    val nombreRol: String,
    val estado: String
)

class CuentaRepository(private val context: Context) {
    private val dbHelper = databasehelpers(context)

    fun obtenerUsuarioPorPin(pin: String): UsuarioInfo? {
        val db = dbHelper.readableDatabase
        var cursor: Cursor? = null

        try {
            val query = """
                SELECT 
                    c.id_cuenta,
                    c.usuario,
                    p.nombres,
                    p.apellidos,
                    r.nombre_rol,
                    c.estado
                FROM cuenta c
                INNER JOIN persona p ON c.id_persona = p.id_persona
                INNER JOIN roles r ON c.id_rol = r.id_rol
                WHERE c.clave = ? AND c.estado = 'ACTIVO'
            """.trimIndent()

            cursor = db.rawQuery(query, arrayOf(pin))

            if (cursor.moveToFirst()) {
                return UsuarioInfo(
                    idCuenta = cursor.getInt(cursor.getColumnIndexOrThrow("id_cuenta")),
                    usuario = cursor.getString(cursor.getColumnIndexOrThrow("usuario")),
                    nombres = cursor.getString(cursor.getColumnIndexOrThrow("nombres")),
                    apellidos = cursor.getString(cursor.getColumnIndexOrThrow("apellidos")),
                    nombreRol = cursor.getString(cursor.getColumnIndexOrThrow("nombre_rol")),
                    estado = cursor.getString(cursor.getColumnIndexOrThrow("estado"))
                )
            }
            return null
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            cursor?.close()
            db.close()
        }
    }

    fun obtenerUsuarioActual(): UsuarioInfo? {
        // Obtener el primer usuario activo (para demostración)
        val db = dbHelper.readableDatabase
        var cursor: Cursor? = null

        try {
            val query = """
                SELECT 
                    c.id_cuenta,
                    c.usuario,
                    p.nombres,
                    p.apellidos,
                    r.nombre_rol,
                    c.estado
                FROM cuenta c
                INNER JOIN persona p ON c.id_persona = p.id_persona
                INNER JOIN roles r ON c.id_rol = r.id_rol
                WHERE c.estado = 'ACTIVO'
                LIMIT 1
            """.trimIndent()

            cursor = db.rawQuery(query, null)

            if (cursor.moveToFirst()) {
                return UsuarioInfo(
                    idCuenta = cursor.getInt(cursor.getColumnIndexOrThrow("id_cuenta")),
                    usuario = cursor.getString(cursor.getColumnIndexOrThrow("usuario")),
                    nombres = cursor.getString(cursor.getColumnIndexOrThrow("nombres")),
                    apellidos = cursor.getString(cursor.getColumnIndexOrThrow("apellidos")),
                    nombreRol = cursor.getString(cursor.getColumnIndexOrThrow("nombre_rol")),
                    estado = cursor.getString(cursor.getColumnIndexOrThrow("estado"))
                )
            }
            return null
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            cursor?.close()
            db.close()
        }
    }
}