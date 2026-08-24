package com.example.scarlet.data.repository

import android.content.Context
import com.example.scarlet.data.model.UsuarioLogueado
import com.example.scarlet.database.databasehelpers

class LoginRepository(context: Context) {

    private val dbHelper = databasehelpers(context)

    /**
     * Valida usuario y clave contra la tabla cuenta y, si son correctos,
     * regresa toda la información de la persona y su rol.
     * Devuelve null si no coinciden las credenciales o la cuenta está inactiva.
     */
    fun iniciarSesion(usuario: String, clave: String): UsuarioLogueado? {
        val db = dbHelper.readableDatabase

        val consulta = """
            SELECT c.id_cuenta, c.usuario, c.estado,
                   p.id_persona, p.nombres, p.apellidos,
                   r.id_rol, r.nombre_rol
            FROM cuenta c
            INNER JOIN persona p ON c.id_persona = p.id_persona
            INNER JOIN roles r ON c.id_rol = r.id_rol
            WHERE c.usuario = ? AND c.clave = ? AND c.estado = 'ACTIVO'
        """.trimIndent()

        val cursor = db.rawQuery(consulta, arrayOf(usuario, clave))

        var resultado: UsuarioLogueado? = null
        if (cursor.moveToFirst()) {
            resultado = UsuarioLogueado(
                id_cuenta = cursor.getInt(cursor.getColumnIndexOrThrow("id_cuenta")),
                usuario = cursor.getString(cursor.getColumnIndexOrThrow("usuario")),
                estado = cursor.getString(cursor.getColumnIndexOrThrow("estado")),
                id_persona = cursor.getInt(cursor.getColumnIndexOrThrow("id_persona")),
                nombres = cursor.getString(cursor.getColumnIndexOrThrow("nombres")),
                apellidos = cursor.getString(cursor.getColumnIndexOrThrow("apellidos")),
                id_rol = cursor.getInt(cursor.getColumnIndexOrThrow("id_rol")),
                nombre_rol = cursor.getString(cursor.getColumnIndexOrThrow("nombre_rol"))
            )
        }
        cursor.close()
        db.close()
        return resultado
    }

    // Cambiar la clave de una cuenta (ej. pantalla "olvidé mi contraseña")
    fun cambiarClave(id_cuenta: Int, nuevaClave: String): Int {
        val db = dbHelper.writableDatabase
        val valores = android.content.ContentValues().apply { put("clave", nuevaClave) }
        val filas = db.update("cuenta", valores, "id_cuenta = ?", arrayOf(id_cuenta.toString()))
        db.close()
        return filas
    }
}
