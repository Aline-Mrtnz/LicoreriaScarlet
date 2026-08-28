package com.example.scarlet.data.repository

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.example.scarlet.data.model.CartItem
import com.example.scarlet.data.model.DetalleVenta
import com.example.scarlet.data.model.VentaResumen
import com.example.scarlet.data.model.Ventas
import com.example.scarlet.database.databasehelpers

class VentasRepository(context: Context) {

    private val dbHelper = databasehelpers(context)
    private val detalleVentaRepository = DetalleVentaRepository(context)

    fun crear(venta: Ventas): Long {
        val db = dbHelper.writableDatabase
        val valores = ContentValues().apply {
            put("fecha_venta", venta.fecha_venta)
            put("total", venta.total)
            put("id_cliente", venta.id_cliente)
            put("id_pago", venta.id_pago)
            put("cuenta_id_cuenta", venta.cuenta_id_cuenta)
        }
        val id = db.insert("ventas", null, valores)
        db.close()
        return id
    }

    fun editar(venta: Ventas): Int {
        val db = dbHelper.writableDatabase
        val valores = ContentValues().apply {
            put("fecha_venta", venta.fecha_venta)
            put("total", venta.total)
            put("id_cliente", venta.id_cliente)
            put("id_pago", venta.id_pago)
            put("cuenta_id_cuenta", venta.cuenta_id_cuenta)
        }
        val filas = db.update("ventas", valores, "id_venta = ?", arrayOf(venta.id_venta.toString()))
        db.close()
        return filas
    }

    fun eliminar(id_venta: Int): Int {
        val db = dbHelper.writableDatabase
        val filas = db.delete("ventas", "id_venta = ?", arrayOf(id_venta.toString()))
        db.close()
        return filas
    }

    fun obtenerPorId(id_venta: Int): Ventas? {
        val db = dbHelper.readableDatabase
        val cursor = db.query("ventas", null, "id_venta = ?", arrayOf(id_venta.toString()), null, null, null)
        var venta: Ventas? = null
        if (cursor.moveToFirst()) venta = mapear(cursor)
        cursor.close()
        db.close()
        return venta
    }

    fun listar(): List<Ventas> {
        val lista = mutableListOf<Ventas>()
        val db = dbHelper.readableDatabase
        val cursor = db.query("ventas", null, null, null, null, null, "fecha_venta DESC")
        if (cursor.moveToFirst()) {
            do { lista.add(mapear(cursor)) } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return lista
    }

    fun listarPorCliente(id_cliente: Int): List<Ventas> {
        val lista = mutableListOf<Ventas>()
        val db = dbHelper.readableDatabase
        val cursor = db.query("ventas", null, "id_cliente = ?", arrayOf(id_cliente.toString()), null, null, "fecha_venta DESC")
        if (cursor.moveToFirst()) {
            do { lista.add(mapear(cursor)) } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return lista
    }

    /**
     * Resultado de intentar registrar una venta completa desde el carrito.
     */
    sealed class ResultadoVenta {
        data class Exito(val idVenta: Long) : ResultadoVenta()
        data class SinStock(val nombreProducto: String) : ResultadoVenta()
        data class Error(val mensaje: String) : ResultadoVenta()
    }

    /**
     * Registra una venta completa de forma atómica: crea la fila en
     * "ventas", una fila en "detalle_venta" por cada producto del carrito
     * y descuenta el stock correspondiente. Si algo falla (por ejemplo,
     * stock insuficiente) se revierte todo y no se guarda nada a medias.
     */
    fun registrarVentaCompleta(
        fecha: String,
        idCliente: Int,
        idPago: Int,
        idCuenta: Int,
        items: List<CartItem>
    ): ResultadoVenta {
        if (items.isEmpty()) {
            return ResultadoVenta.Error("El carrito está vacío")
        }

        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            // 1. Verificar stock actual de cada producto (dentro de la transacción)
            for (item in items) {
                val cursor = db.query(
                    "productos", arrayOf("stock", "nombre_producto"),
                    "id_producto = ?", arrayOf(item.idProducto.toString()),
                    null, null, null
                )
                var stockActual = -1
                var nombre = item.nombre
                if (cursor.moveToFirst()) {
                    stockActual = cursor.getInt(0)
                    nombre = cursor.getString(1)
                }
                cursor.close()

                if (stockActual < 0 || stockActual < item.cantidad) {
                    return ResultadoVenta.SinStock(nombre)
                }
            }

            // 2. Insertar la venta
            val total = items.sumOf { it.subtotal }
            val valoresVenta = ContentValues().apply {
                put("fecha_venta", fecha)
                put("total", total)
                put("descuento", 0.0)
                put("id_cliente", idCliente)
                put("id_pago", idPago)
                put("cuenta_id_cuenta", idCuenta)
            }
            val idVenta = db.insert("ventas", null, valoresVenta)
            if (idVenta == -1L) {
                return ResultadoVenta.Error("No se pudo registrar la venta")
            }

            // 3. Insertar el detalle y descontar stock por cada producto
            for (item in items) {
                val detalle = DetalleVenta(
                    cantidad = item.cantidad,
                    precio = item.precioUnitario,
                    id_venta = idVenta.toInt(),
                    id_producto = item.idProducto
                )
                val idDetalle = detalleVentaRepository.crear(db, detalle)
                if (idDetalle == -1L) {
                    return ResultadoVenta.Error("No se pudo registrar el detalle de ${item.nombre}")
                }

                db.execSQL(
                    "UPDATE productos SET stock = stock - ? WHERE id_producto = ?",
                    arrayOf(item.cantidad, item.idProducto)
                )
            }

            db.setTransactionSuccessful()
            return ResultadoVenta.Exito(idVenta)
        } catch (e: Exception) {
            e.printStackTrace()
            return ResultadoVenta.Error(e.message ?: "Error desconocido al registrar la venta")
        } finally {
            // db.setTransactionSuccessful() solo se llamó en el camino feliz;
            // en cualquier otro caso endTransaction() revierte todo lo hecho
            // dentro de este método (venta, detalles y descuentos de stock).
            db.endTransaction()
            db.close()
        }
    }

    /**
     * Lista de ventas ya "aplanadas" con el nombre del cliente y el tipo de
     * pago resueltos, para mostrar en el historial de Ventas.
     * Si [desde]/[hasta] son null, trae todas las ventas.
     */
    fun listarResumen(desde: String? = null, hasta: String? = null, limite: Int? = null): List<VentaResumen> {
        val lista = mutableListOf<VentaResumen>()
        val db = dbHelper.readableDatabase

        val where = if (desde != null && hasta != null) "WHERE v.fecha_venta BETWEEN ? AND ?" else ""
        val limitClause = if (limite != null) "LIMIT $limite" else ""
        val query = """
            SELECT v.id_venta, v.fecha_venta, v.total, v.descuento,
                   p.nombres || ' ' || p.apellidos AS nombre_cliente,
                   pg.tipo_pago
            FROM ventas v
            INNER JOIN persona p ON v.id_cliente = p.id_persona
            INNER JOIN pagos pg ON v.id_pago = pg.id_pago
            $where
            ORDER BY v.fecha_venta DESC
            $limitClause
        """.trimIndent()

        val args = if (desde != null && hasta != null) arrayOf(desde, hasta) else null
        val cursor = db.rawQuery(query, args)
        if (cursor.moveToFirst()) {
            do {
                lista.add(
                    VentaResumen(
                        idVenta = cursor.getInt(cursor.getColumnIndexOrThrow("id_venta")),
                        fechaVenta = cursor.getString(cursor.getColumnIndexOrThrow("fecha_venta")),
                        total = cursor.getDouble(cursor.getColumnIndexOrThrow("total")),
                        descuento = cursor.getDouble(cursor.getColumnIndexOrThrow("descuento")),
                        nombreCliente = cursor.getString(cursor.getColumnIndexOrThrow("nombre_cliente")),
                        tipoPago = cursor.getString(cursor.getColumnIndexOrThrow("tipo_pago"))
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return lista
    }

    /** Total vendido entre dos fechas (formato yyyy-MM-dd HH:mm:ss). */
    fun totalEntreFechas(desde: String, hasta: String): Double {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT COALESCE(SUM(total), 0) FROM ventas WHERE fecha_venta BETWEEN ? AND ?",
            arrayOf(desde, hasta)
        )
        var total = 0.0
        if (cursor.moveToFirst()) total = cursor.getDouble(0)
        cursor.close()
        db.close()
        return total
    }

    /** Ganancia (venta - costo) entre dos fechas, según detalle_venta y precio_compra. */
    fun gananciaEntreFechas(desde: String, hasta: String): Double {
        val db = dbHelper.readableDatabase
        val query = """
            SELECT COALESCE(SUM((d.precio_unitario - COALESCE(p.precio_compra, 0)) * d.cantidad), 0)
            FROM detalle_venta d
            INNER JOIN ventas v ON d.id_venta = v.id_venta
            INNER JOIN productos p ON d.id_producto = p.id_producto
            WHERE v.fecha_venta BETWEEN ? AND ?
        """.trimIndent()
        val cursor = db.rawQuery(query, arrayOf(desde, hasta))
        var ganancia = 0.0
        if (cursor.moveToFirst()) ganancia = cursor.getDouble(0)
        cursor.close()
        db.close()
        return ganancia
    }

    /** Número de ventas registradas entre dos fechas (para el ticket promedio). */
    fun cantidadVentasEntreFechas(desde: String, hasta: String): Int {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM ventas WHERE fecha_venta BETWEEN ? AND ?",
            arrayOf(desde, hasta)
        )
        var cantidad = 0
        if (cursor.moveToFirst()) cantidad = cursor.getInt(0)
        cursor.close()
        db.close()
        return cantidad
    }

    /** Total vendido por cada día (clave "yyyy-MM-dd") dentro de la lista de días dada. */
    fun totalesPorDia(dias: List<String>): Map<String, Double> {
        if (dias.isEmpty()) return emptyMap()
        val resultado = dias.associateWith { 0.0 }.toMutableMap()
        val db = dbHelper.readableDatabase
        val placeholders = dias.joinToString(",") { "?" }
        val query = """
            SELECT substr(fecha_venta, 1, 10) AS dia, COALESCE(SUM(total), 0) AS total
            FROM ventas
            WHERE substr(fecha_venta, 1, 10) IN ($placeholders)
            GROUP BY dia
        """.trimIndent()
        val cursor = db.rawQuery(query, dias.toTypedArray())
        if (cursor.moveToFirst()) {
            do {
                val dia = cursor.getString(0)
                val total = cursor.getDouble(1)
                resultado[dia] = total
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return resultado
    }

    private fun mapear(cursor: Cursor): Ventas = Ventas(
        id_venta = cursor.getInt(cursor.getColumnIndexOrThrow("id_venta")),
        fecha_venta = cursor.getString(cursor.getColumnIndexOrThrow("fecha_venta")),
        total = cursor.getDouble(cursor.getColumnIndexOrThrow("total")),
        id_cliente = cursor.getInt(cursor.getColumnIndexOrThrow("id_cliente")),
        id_pago = cursor.getInt(cursor.getColumnIndexOrThrow("id_pago")),
        cuenta_id_cuenta = cursor.getInt(cursor.getColumnIndexOrThrow("cuenta_id_cuenta"))
    )
}
