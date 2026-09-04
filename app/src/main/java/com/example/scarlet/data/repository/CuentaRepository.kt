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
data class CajeroInfo(
    val idCuenta: Int,
    val idPersona: Int,
    val usuario: String,
    val nombres: String,
    val apellidos: String,
    val ci: String,
    val telefono: String,
    val estado: String
)

sealed class ResultadoCajero {
    data class Exito(val idCuenta: Long) : ResultadoCajero()
    object UsuarioDuplicado : ResultadoCajero()
    object CiDuplicado : ResultadoCajero()
    data class Error(val mensaje: String) : ResultadoCajero()
}
class CuentaRepository(private val context: Context) {

    private val dbHelper = databasehelpers(context)

    fun obtenerUsuarioPorPin(pin: String): UsuarioInfo? {

        val db = dbHelper.readableDatabase
        var cursor: Cursor? = null

        try {

            // Ya no se puede filtrar con "WHERE clave = ?": el PIN se guarda
            // hasheado (PBKDF2 + salt por cuenta), así que dos cuentas con el
            // mismo PIN tienen hashes distintos. Recorremos las cuentas
            // activas y comparamos con PasswordUtils.verificar (que también
            // acepta el formato viejo en texto plano por compatibilidad).
            val query = """
                SELECT 
                    c.id_cuenta,
                    c.id_persona,
                    c.usuario,
                    c.clave,
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
                WHERE c.estado = 'ACTIVO'
            """.trimIndent()

            cursor = db.rawQuery(query, null)

            while (cursor.moveToNext()) {
                val claveGuardada = cursor.getString(cursor.getColumnIndexOrThrow("clave"))
                if (!com.example.scarlet.util.PasswordUtils.verificar(pin, claveGuardada)) continue

                return UsuarioInfo(
                    idCuenta = cursor.getInt(cursor.getColumnIndexOrThrow("id_cuenta")),
                    idPersona = cursor.getInt(cursor.getColumnIndexOrThrow("id_persona")),
                    usuario = cursor.getString(cursor.getColumnIndexOrThrow("usuario")),
                    nombres = cursor.getString(cursor.getColumnIndexOrThrow("nombres")),
                    apellidos = cursor.getString(cursor.getColumnIndexOrThrow("apellidos")),
                    telefono = cursor.getString(cursor.getColumnIndexOrThrow("telefono")),
                    nombreRol = cursor.getString(cursor.getColumnIndexOrThrow("nombre_rol")),
                    estado = cursor.getString(cursor.getColumnIndexOrThrow("estado"))
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

                /*if (nuevaClave.isNotEmpty()) {
                    put("clave", nuevaClave)
                }*/
                if (nuevaClave.isNotEmpty()) {
                    put("clave", com.example.scarlet.util.PasswordUtils.hash(nuevaClave))
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
    /** Cuentas con rol "Vendedor" (mostrado como "Cajero" en la UI). Incluye inactivas para poder reactivarlas. */
    fun listarCajeros(): List<CajeroInfo> {
        val lista = mutableListOf<CajeroInfo>()
        val db = dbHelper.readableDatabase
        val query = """
            SELECT c.id_cuenta, c.id_persona, c.usuario, c.estado,
                   p.nombres, p.apellidos, p.ci, p.telefono
            FROM cuenta c
            INNER JOIN persona p ON c.id_persona = p.id_persona
            INNER JOIN roles r ON c.id_rol = r.id_rol
            WHERE r.nombre_rol = 'Vendedor'
            ORDER BY p.nombres ASC
        """.trimIndent()
        val cursor = db.rawQuery(query, null)
        try {
            while (cursor.moveToNext()) {
                lista.add(
                    CajeroInfo(
                        idCuenta = cursor.getInt(cursor.getColumnIndexOrThrow("id_cuenta")),
                        idPersona = cursor.getInt(cursor.getColumnIndexOrThrow("id_persona")),
                        usuario = cursor.getString(cursor.getColumnIndexOrThrow("usuario")),
                        nombres = cursor.getString(cursor.getColumnIndexOrThrow("nombres")),
                        apellidos = cursor.getString(cursor.getColumnIndexOrThrow("apellidos")),
                        ci = cursor.getString(cursor.getColumnIndexOrThrow("ci")) ?: "",
                        telefono = cursor.getString(cursor.getColumnIndexOrThrow("telefono")) ?: "",
                        estado = cursor.getString(cursor.getColumnIndexOrThrow("estado"))
                    )
                )
            }
        } finally {
            cursor.close()
            db.close()
        }
        return lista
    }

    /** Crea una cuenta de Cajero (rol "Vendedor") junto con su persona asociada. Solo debe llamarse si Session.esAdmin. */
    fun crearCajero(
        nombres: String, apellidos: String, ci: String,
        telefono: String, usuario: String, pin: String
    ): ResultadoCajero {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            val cursorUsuario = db.rawQuery("SELECT id_cuenta FROM cuenta WHERE usuario = ?", arrayOf(usuario))
            val usuarioExiste = cursorUsuario.moveToFirst()
            cursorUsuario.close()
            if (usuarioExiste) return ResultadoCajero.UsuarioDuplicado

            if (ci.isNotBlank()) {
                val cursorCi = db.rawQuery("SELECT id_persona FROM persona WHERE ci = ?", arrayOf(ci))
                val ciExiste = cursorCi.moveToFirst()
                cursorCi.close()
                if (ciExiste) return ResultadoCajero.CiDuplicado
            }

            val cursorRol = db.rawQuery("SELECT id_rol FROM roles WHERE nombre_rol = 'Vendedor'", null)
            if (!cursorRol.moveToFirst()) {
                cursorRol.close()
                return ResultadoCajero.Error("No existe el rol Vendedor/Cajero en el sistema")
            }
            val idRolCajero = cursorRol.getInt(cursorRol.getColumnIndexOrThrow("id_rol"))
            cursorRol.close()

            val idPersona = db.insert("persona", null, ContentValues().apply {
                put("nombres", nombres)
                put("apellidos", apellidos)
                put("ci", if (ci.isBlank()) null else ci)
                put("telefono", telefono)
            })
            if (idPersona <= 0) return ResultadoCajero.Error("No se pudo registrar los datos personales")

            val idCuenta = db.insert("cuenta", null, ContentValues().apply {
                put("usuario", usuario)
                put("clave", com.example.scarlet.util.PasswordUtils.hash(pin))
                put("estado", "ACTIVO")
                put("id_persona", idPersona)
                put("id_rol", idRolCajero)
            })
            if (idCuenta <= 0) return ResultadoCajero.Error("No se pudo crear la cuenta")

            db.setTransactionSuccessful()
            return ResultadoCajero.Exito(idCuenta)
        } catch (e: Exception) {
            e.printStackTrace()
            return ResultadoCajero.Error(e.message ?: "Error desconocido")
        } finally {
            db.endTransaction()
            db.close()
        }
    }

    /** Edita datos y, opcionalmente (si no viene vacío), el PIN de un Cajero. */
    fun editarCajero(
        idCuenta: Int, idPersona: Int, nombres: String, apellidos: String,
        telefono: String, usuario: String, nuevoPinOpcional: String
    ): ResultadoCajero {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            val cursorUsuario = db.rawQuery(
                "SELECT id_cuenta FROM cuenta WHERE usuario = ? AND id_cuenta != ?",
                arrayOf(usuario, idCuenta.toString())
            )
            val usuarioExiste = cursorUsuario.moveToFirst()
            cursorUsuario.close()
            if (usuarioExiste) return ResultadoCajero.UsuarioDuplicado

            db.update("persona", ContentValues().apply {
                put("nombres", nombres)
                put("apellidos", apellidos)
                put("telefono", telefono)
            }, "id_persona = ?", arrayOf(idPersona.toString()))

            val cuentaValues = ContentValues().apply {
                put("usuario", usuario)
                if (nuevoPinOpcional.isNotEmpty()) {
                    put("clave", com.example.scarlet.util.PasswordUtils.hash(nuevoPinOpcional))
                }
            }
            db.update("cuenta", cuentaValues, "id_cuenta = ?", arrayOf(idCuenta.toString()))

            db.setTransactionSuccessful()
            return ResultadoCajero.Exito(idCuenta.toLong())
        } catch (e: Exception) {
            e.printStackTrace()
            return ResultadoCajero.Error(e.message ?: "Error desconocido")
        } finally {
            db.endTransaction()
            db.close()
        }
    }

    /** Activa/desactiva una cuenta de Cajero (no se elimina: conserva su historial de ventas). */
    fun cambiarEstadoCajero(idCuenta: Int, activar: Boolean): Boolean {
        val db = dbHelper.writableDatabase
        return try {
            db.update(
                "cuenta",
                ContentValues().apply { put("estado", if (activar) "ACTIVO" else "INACTIVO") },
                "id_cuenta = ?", arrayOf(idCuenta.toString())
            ) > 0
        } finally {
            db.close()
        }
    }
}