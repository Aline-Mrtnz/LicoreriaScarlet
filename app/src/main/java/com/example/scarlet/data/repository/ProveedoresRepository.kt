// app/src/main/java/com/example/scarlet/data/repository/ProveedoresRepository.kt

package com.example.scarlet.data.repository

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.example.scarlet.data.model.Proveedor
import com.example.scarlet.data.model.ProveedorProducto
import com.example.scarlet.database.databasehelpers

class ProveedoresRepository(context: Context) {

    private val dbHelper = databasehelpers(context)

    companion object {
        const val ESTADO_ACTIVO = "ACTIVO"
        const val ESTADO_INACTIVO = "INACTIVO"
    }

    // =============================================
    // CRUD PROVEEDORES
    // =============================================

    fun crear(proveedor: Proveedor): Long {
        val db = dbHelper.writableDatabase
        val valores = ContentValues().apply {
            put("razon_social", proveedor.razonSocial)
            put("rfc_nit", proveedor.rfcNit)
            put("condicion_pago", proveedor.condicionPago)
            put("marcas_asociadas", proveedor.marcasAsociadas)
            put("contacto_ejecutivo", proveedor.contactoEjecutivo)
            put("telefono_contacto", proveedor.telefonoContacto)
            put("estado", proveedor.estado)
        }
        val id = db.insert("proveedores", null, valores)
        db.close()
        return id
    }

    fun editar(proveedor: Proveedor): Int {
        val db = dbHelper.writableDatabase
        val valores = ContentValues().apply {
            put("razon_social", proveedor.razonSocial)
            put("rfc_nit", proveedor.rfcNit)
            put("condicion_pago", proveedor.condicionPago)
            put("marcas_asociadas", proveedor.marcasAsociadas)
            put("contacto_ejecutivo", proveedor.contactoEjecutivo)
            put("telefono_contacto", proveedor.telefonoContacto)
        }
        val filas = db.update(
            "proveedores", valores,
            "id_proveedor = ?", arrayOf(proveedor.idProveedor.toString())
        )
        db.close()
        return filas
    }

    fun cambiarEstado(idProveedor: Int, nuevoEstado: String): Int {
        val db = dbHelper.writableDatabase
        val valores = ContentValues().apply { put("estado", nuevoEstado) }
        val filas = db.update(
            "proveedores", valores,
            "id_proveedor = ?", arrayOf(idProveedor.toString())
        )
        db.close()
        return filas
    }

    fun eliminar(idProveedor: Int): Int {
        val db = dbHelper.writableDatabase
        db.delete("proveedor_productos", "id_proveedor = ?", arrayOf(idProveedor.toString()))
        val filas = db.delete("proveedores", "id_proveedor = ?", arrayOf(idProveedor.toString()))
        db.close()
        return filas
    }

    fun obtenerPorId(idProveedor: Int): Proveedor? {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            "proveedores", null,
            "id_proveedor = ?", arrayOf(idProveedor.toString()),
            null, null, null
        )
        var proveedor: Proveedor? = null
        if (cursor.moveToFirst()) {
            proveedor = mapear(cursor, contarProductos(db, cursor.getInt(cursor.getColumnIndexOrThrow("id_proveedor"))))
        }
        cursor.close()
        db.close()
        return proveedor
    }

    // filtroEstado: null = todos, o ESTADO_ACTIVO / ESTADO_INACTIVO
    // busqueda: coincide contra razón social, RFC/NIT o marcas asociadas
    fun listar(filtroEstado: String? = null, busqueda: String? = null): List<Proveedor> {
        val lista = mutableListOf<Proveedor>()
        val db = dbHelper.readableDatabase

        val condiciones = mutableListOf<String>()
        val args = mutableListOf<String>()

        if (!filtroEstado.isNullOrBlank()) {
            condiciones.add("estado = ?")
            args.add(filtroEstado)
        }

        if (!busqueda.isNullOrBlank()) {
            condiciones.add("(razon_social LIKE ? OR rfc_nit LIKE ? OR marcas_asociadas LIKE ?)")
            val comodin = "%$busqueda%"
            args.add(comodin)
            args.add(comodin)
            args.add(comodin)
        }

        val where = if (condiciones.isNotEmpty()) condiciones.joinToString(" AND ") else null
        val whereArgs = if (args.isNotEmpty()) args.toTypedArray() else null

        val cursor = db.query(
            "proveedores", null,
            where, whereArgs,
            null, null, "razon_social ASC"
        )

        if (cursor.moveToFirst()) {
            do {
                val idProveedor = cursor.getInt(cursor.getColumnIndexOrThrow("id_proveedor"))
                lista.add(mapear(cursor, contarProductos(db, idProveedor)))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return lista
    }

    // Devuelve Pair(activos, inactivos)
    fun contarPorEstado(): Pair<Int, Int> {
        val db = dbHelper.readableDatabase
        var activos = 0
        var inactivos = 0

        val cursor = db.rawQuery(
            "SELECT estado, COUNT(*) as total FROM proveedores GROUP BY estado", null
        )
        if (cursor.moveToFirst()) {
            do {
                val estado = cursor.getString(cursor.getColumnIndexOrThrow("estado"))
                val total = cursor.getInt(cursor.getColumnIndexOrThrow("total"))
                if (estado.equals(ESTADO_ACTIVO, ignoreCase = true)) activos = total
                else inactivos += total
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return Pair(activos, inactivos)
    }

    private fun contarProductos(db: android.database.sqlite.SQLiteDatabase, idProveedor: Int): Int {
        val cursor = db.rawQuery(
            "SELECT COUNT(*) as total FROM proveedor_productos WHERE id_proveedor = ?",
            arrayOf(idProveedor.toString())
        )
        var total = 0
        if (cursor.moveToFirst()) {
            total = cursor.getInt(cursor.getColumnIndexOrThrow("total"))
        }
        cursor.close()
        return total
    }

    private fun mapear(cursor: Cursor, cantidadProductos: Int): Proveedor = Proveedor(
        idProveedor = cursor.getInt(cursor.getColumnIndexOrThrow("id_proveedor")),
        razonSocial = cursor.getString(cursor.getColumnIndexOrThrow("razon_social")),
        rfcNit = cursor.getString(cursor.getColumnIndexOrThrow("rfc_nit")) ?: "",
        condicionPago = cursor.getString(cursor.getColumnIndexOrThrow("condicion_pago")) ?: "",
        marcasAsociadas = cursor.getString(cursor.getColumnIndexOrThrow("marcas_asociadas")) ?: "",
        contactoEjecutivo = cursor.getString(cursor.getColumnIndexOrThrow("contacto_ejecutivo")) ?: "",
        telefonoContacto = cursor.getString(cursor.getColumnIndexOrThrow("telefono_contacto")) ?: "",
        estado = cursor.getString(cursor.getColumnIndexOrThrow("estado")),
        cantidadProductos = cantidadProductos
    )

    // =============================================
    // PRODUCTOS VINCULADOS AL PROVEEDOR
    // =============================================

    fun listarProductosDeProveedor(idProveedor: Int): List<ProveedorProducto> {
        val lista = mutableListOf<ProveedorProducto>()
        val db = dbHelper.readableDatabase
        val query = """
            SELECT pp.id_proveedor_producto, pp.id_proveedor, pp.id_producto,
                   pp.precio_pactado, p.nombre_producto
            FROM proveedor_productos pp
            INNER JOIN productos p ON pp.id_producto = p.id_producto
            WHERE pp.id_proveedor = ?
            ORDER BY p.nombre_producto ASC
        """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(idProveedor.toString()))
        if (cursor.moveToFirst()) {
            do {
                lista.add(
                    ProveedorProducto(
                        idProveedorProducto = cursor.getInt(cursor.getColumnIndexOrThrow("id_proveedor_producto")),
                        idProveedor = cursor.getInt(cursor.getColumnIndexOrThrow("id_proveedor")),
                        idProducto = cursor.getInt(cursor.getColumnIndexOrThrow("id_producto")),
                        precioPactado = cursor.getDouble(cursor.getColumnIndexOrThrow("precio_pactado")),
                        nombreProducto = cursor.getString(cursor.getColumnIndexOrThrow("nombre_producto"))
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return lista
    }

    // Devuelve el id insertado, o -1 si el producto ya estaba vinculado a ese proveedor.
    fun agregarProductoAProveedor(idProveedor: Int, idProducto: Int, precioPactado: Double): Long {
        val db = dbHelper.writableDatabase

        // Evitar duplicados (ya existe UNIQUE en la tabla, pero comprobamos antes
        // para poder avisar al usuario con un mensaje claro desde la Activity).
        val existente = db.query(
            "proveedor_productos", arrayOf("id_proveedor_producto"),
            "id_proveedor = ? AND id_producto = ?",
            arrayOf(idProveedor.toString(), idProducto.toString()),
            null, null, null
        )
        val yaExiste = existente.count > 0
        existente.close()

        if (yaExiste) {
            db.close()
            return -1
        }

        val valores = ContentValues().apply {
            put("id_proveedor", idProveedor)
            put("id_producto", idProducto)
            put("precio_pactado", precioPactado)
        }
        val id = db.insert("proveedor_productos", null, valores)
        db.close()
        return id
    }

    fun quitarProductoDeProveedor(idProveedorProducto: Int): Int {
        val db = dbHelper.writableDatabase
        val filas = db.delete(
            "proveedor_productos",
            "id_proveedor_producto = ?",
            arrayOf(idProveedorProducto.toString())
        )
        db.close()
        return filas
    }
}