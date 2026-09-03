package com.example.scarlet.data.repository

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.example.scarlet.database.databasehelpers

data class UsuarioInfo(
    val idCuenta: Int,
    val idPersona: Int,
    val usuario: String,
    val nombres: String,
    val apellidos: String,
    val telefono: String,
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
                    c.id_persona,
                    c.usuario,
                    p.nombres,
                    p.apellidos,
                    p.telefono,
                    r.nombre_rol,
                    c.estado
                FROM cuenta c
                INNER JOIN persona p 
                    ON c.id_persona = p.id_persona
                INNER JOIN roles r 
                    ON c.id_rol = r.id_rol
                WHERE c.clave = ?
                AND c.estado = 'ACTIVO'
            """.trimIndent()

            cursor = db.rawQuery(query, arrayOf(pin))

            if (cursor.moveToFirst()) {

                return UsuarioInfo(
                    idCuenta = cursor.getInt(
                        cursor.getColumnIndexOrThrow("id_cuenta")
                    ),

                    idPersona = cursor.getInt(
                        cursor.getColumnIndexOrThrow("id_persona")
                    ),

                    usuario = cursor.getString(
                        cursor.getColumnIndexOrThrow("usuario")
                    ),

                    nombres = cursor.getString(
                        cursor.getColumnIndexOrThrow("nombres")
                    ),

                    apellidos = cursor.getString(
                        cursor.getColumnIndexOrThrow("apellidos")
                    ),

                    telefono = cursor.getString(
                        cursor.getColumnIndexOrThrow("telefono")
                    ),

                    nombreRol = cursor.getString(
                        cursor.getColumnIndexOrThrow("nombre_rol")
                    ),

                    estado = cursor.getString(
                        cursor.getColumnIndexOrThrow("estado")
                    )
                )
            }

            return null

        } finally {

            cursor?.close()
            db.close()
        }
    }

    fun obtenerUsuarioActual(): UsuarioInfo? {

        val idCuenta = com.example.scarlet.util.Session.idCuenta

        if (idCuenta <= 0) {
            return null
        }

        val db = dbHelper.readableDatabase
        var cursor: Cursor? = null

        try {

            val query = """
                SELECT 
                    c.id_cuenta,
                    c.id_persona,
                    c.usuario,
                    p.nombres,
                    p.apellidos,
                    p.telefono,
                    r.nombre_rol,
                    c.estado
                FROM cuenta c
                INNER JOIN persona p 
                    ON c.id_persona = p.id_persona
                INNER JOIN roles r 
                    ON c.id_rol = r.id_rol
                WHERE c.id_cuenta = ?
                AND c.estado = 'ACTIVO'
            """.trimIndent()

            cursor = db.rawQuery(
                query,
                arrayOf(idCuenta.toString())
            )

            if (cursor.moveToFirst()) {

                return UsuarioInfo(
                    idCuenta = cursor.getInt(
                        cursor.getColumnIndexOrThrow("id_cuenta")
                    ),

                    idPersona = cursor.getInt(
                        cursor.getColumnIndexOrThrow("id_persona")
                    ),

                    usuario = cursor.getString(
                        cursor.getColumnIndexOrThrow("usuario")
                    ),

                    nombres = cursor.getString(
                        cursor.getColumnIndexOrThrow("nombres")
                    ),

                    apellidos = cursor.getString(
                        cursor.getColumnIndexOrThrow("apellidos")
                    ),

                    telefono = cursor.getString(
                        cursor.getColumnIndexOrThrow("telefono")
                    ),

                    nombreRol = cursor.getString(
                        cursor.getColumnIndexOrThrow("nombre_rol")
                    ),

                    estado = cursor.getString(
                        cursor.getColumnIndexOrThrow("estado")
                    )
                )
            }

            return null

        } finally {

            cursor?.close()
            db.close()
        }
    }

    fun actualizarCuenta(
        idCuenta: Int,
        idPersona: Int,
        nombres: String,
        apellidos: String,
        usuario: String,
        telefono: String,
        nuevaClave: String
    ): Boolean {

        val db = dbHelper.writableDatabase

        db.beginTransaction()

        try {

            // ============================================
            // VERIFICAR QUE EL USUARIO NO PERTENEZCA
            // A OTRA CUENTA
            // ============================================

            val cursor = db.rawQuery(
                """
                SELECT id_cuenta
                FROM cuenta
                WHERE usuario = ?
                AND id_cuenta != ?
                """.trimIndent(),
                arrayOf(
                    usuario,
                    idCuenta.toString()
                )
            )

            val usuarioExiste = cursor.moveToFirst()
            cursor.close()

            if (usuarioExiste) {
                return false
            }

            // ============================================
            // ACTUALIZAR PERSONA
            // ============================================

            val personaValues = ContentValues().apply {

                put("nombres", nombres)
                put("apellidos", apellidos)
                put("telefono", telefono)
            }

            val personaFilas = db.update(
                "persona",
                personaValues,
                "id_persona = ?",
                arrayOf(idPersona.toString())
            )

            if (personaFilas <= 0) {
                return false
            }

            // ============================================
            // ACTUALIZAR CUENTA
            // ============================================

            val cuentaValues = ContentValues().apply {

                put("usuario", usuario)

                if (nuevaClave.isNotEmpty()) {
                    put("clave", nuevaClave)
                }
            }

            val cuentaFilas = db.update(
                "cuenta",
                cuentaValues,
                "id_cuenta = ?",
                arrayOf(idCuenta.toString())
            )

            if (cuentaFilas <= 0) {
                return false
            }

            db.setTransactionSuccessful()

            return true

        } catch (e: Exception) {

            e.printStackTrace()
            return false

        } finally {

            db.endTransaction()
            db.close()
        }
    }
}