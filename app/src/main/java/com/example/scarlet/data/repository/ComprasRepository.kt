package com.example.scarlet.data.repository

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.example.scarlet.data.model.Compra
import com.example.scarlet.data.model.DetalleCompra
import com.example.scarlet.data.model.PagoCompra
import com.example.scarlet.database.databasehelpers
import com.example.scarlet.util.FechaUtils

class ComprasRepository(private val context: Context) {

    private val dbHelper = databasehelpers(context)
    private val inventarioRepository = InventarioRepository(context)

    data class Estadisticas(
        val ordenesAbiertas: Int = 0,
        val pendientePago: Double = 0.0,
        val totalRecibido: Double = 0.0,
        val pedidosRecibidos: Int = 0
    )

    // =============================================
    // CREAR COMPRA
    // =============================================

    /** Crea la orden de compra en estado PENDIENTE junto con su detalle. Devuelve el id generado (-1 si falla). */
    fun crearCompra(
        idProveedor: Int,
        observacion: String?,
        cuentaIdCuenta: Int,
        detalles: List<DetalleCompra>
    ): Long {
        if (detalles.isEmpty()) return -1L

        val db = dbHelper.writableDatabase
        var idCompra = -1L
        db.beginTransaction()
        try {
            var siguiente = 270884
            val countCursor = db.rawQuery("SELECT COUNT(*) FROM compras", null)
            countCursor.use { if (it.moveToFirst()) siguiente = 270884 + it.getInt(0) }
            val codigo = "OC-$siguiente"
            val total = detalles.sumOf { it.subtotal }

            val valoresCompra = ContentValues().apply {
                put("codigo", codigo)
                put("fecha_emision", FechaUtils.ahora())
                put("observacion", observacion)
                put("estado", "PENDIENTE")
                put("total", total)
                put("id_proveedor", idProveedor)
                put("cuenta_id_cuenta", cuentaIdCuenta)
            }
            idCompra = db.insert("compras", null, valoresCompra)

            if (idCompra > 0) {
                detalles.forEach { detalle ->
                    val valoresDetalle = ContentValues().apply {
                        put("cantidad", detalle.cantidad)
                        put("precio_unitario", detalle.precioUnitario)
                        put("subtotal", detalle.subtotal)
                        put("id_compra", idCompra)
                        put("id_producto", detalle.idProducto)
                    }
                    db.insert("detalle_compra", null, valoresDetalle)
                }
                db.setTransactionSuccessful()
            }
        } finally {
            db.endTransaction()
            db.close()
        }
        return idCompra
    }

    // =============================================
    // CONSULTAS
    // =============================================

    fun listarCompras(filtroEstado: String? = null): List<Compra> {
        val lista = mutableListOf<Compra>()
        val db = dbHelper.readableDatabase

        val where = if (filtroEstado != null && filtroEstado != "Todas") "WHERE co.estado = ?" else ""
        val args = if (where.isNotEmpty()) arrayOf(filtroEstado) else null

        val query = """
            SELECT
                co.id_compra, co.codigo, co.fecha_emision, co.observacion, co.estado, co.total,
                co.id_proveedor, co.cuenta_id_cuenta,
                pr.razon_social, pr.rfc_nit, pr.condicion_pago,
                COALESCE((SELECT SUM(pc.monto) FROM pagos_compra pc WHERE pc.id_compra = co.id_compra), 0) AS total_pagado
            FROM compras co
            INNER JOIN proveedores pr ON co.id_proveedor = pr.id_proveedor
            $where
            ORDER BY co.id_compra DESC
        """.trimIndent()

        val cursor = db.rawQuery(query, args)
        try {
            while (cursor.moveToNext()) lista.add(mapearCompra(cursor))
        } finally {
            cursor.close()
            db.close()
        }
        return lista
    }

    fun obtenerCompra(idCompra: Int): Compra? {
        val db = dbHelper.readableDatabase
        val query = """
            SELECT
                co.id_compra, co.codigo, co.fecha_emision, co.observacion, co.estado, co.total,
                co.id_proveedor, co.cuenta_id_cuenta,
                pr.razon_social, pr.rfc_nit, pr.condicion_pago,
                COALESCE((SELECT SUM(pc.monto) FROM pagos_compra pc WHERE pc.id_compra = co.id_compra), 0) AS total_pagado
            FROM compras co
            INNER JOIN proveedores pr ON co.id_proveedor = pr.id_proveedor
            WHERE co.id_compra = ?
        """.trimIndent()
        val cursor = db.rawQuery(query, arrayOf(idCompra.toString()))
        var compra: Compra? = null
        try {
            if (cursor.moveToFirst()) compra = mapearCompra(cursor)
        } finally {
            cursor.close()
            db.close()
        }
        return compra
    }

    private fun mapearCompra(c: Cursor): Compra {
        return Compra(
            idCompra = c.getInt(c.getColumnIndexOrThrow("id_compra")),
            codigo = c.getString(c.getColumnIndexOrThrow("codigo")),
            fechaEmision = c.getString(c.getColumnIndexOrThrow("fecha_emision")),
            observacion = c.getString(c.getColumnIndexOrThrow("observacion")),
            estado = c.getString(c.getColumnIndexOrThrow("estado")),
            total = c.getDouble(c.getColumnIndexOrThrow("total")),
            idProveedor = c.getInt(c.getColumnIndexOrThrow("id_proveedor")),
            cuentaIdCuenta = c.getInt(c.getColumnIndexOrThrow("cuenta_id_cuenta")),
            razonSocialProveedor = c.getString(c.getColumnIndexOrThrow("razon_social")),
            rfcNitProveedor = c.getString(c.getColumnIndexOrThrow("rfc_nit")),
            condicionPagoProveedor = c.getString(c.getColumnIndexOrThrow("condicion_pago")),
            totalPagado = c.getDouble(c.getColumnIndexOrThrow("total_pagado"))
        )
    }

    fun listarDetalle(idCompra: Int): List<DetalleCompra> {
        val lista = mutableListOf<DetalleCompra>()
        val db = dbHelper.readableDatabase
        val query = """
            SELECT dc.id_detalle_compra, dc.cantidad, dc.precio_unitario, dc.subtotal,
                   dc.id_compra, dc.id_producto, p.nombre_producto
            FROM detalle_compra dc
            INNER JOIN productos p ON dc.id_producto = p.id_producto
            WHERE dc.id_compra = ?
        """.trimIndent()
        val cursor = db.rawQuery(query, arrayOf(idCompra.toString()))
        try {
            while (cursor.moveToNext()) {
                lista.add(
                    DetalleCompra(
                        idDetalleCompra = cursor.getInt(cursor.getColumnIndexOrThrow("id_detalle_compra")),
                        cantidad = cursor.getInt(cursor.getColumnIndexOrThrow("cantidad")),
                        precioUnitario = cursor.getDouble(cursor.getColumnIndexOrThrow("precio_unitario")),
                        subtotal = cursor.getDouble(cursor.getColumnIndexOrThrow("subtotal")),
                        idCompra = cursor.getInt(cursor.getColumnIndexOrThrow("id_compra")),
                        idProducto = cursor.getInt(cursor.getColumnIndexOrThrow("id_producto")),
                        nombreProducto = cursor.getString(cursor.getColumnIndexOrThrow("nombre_producto"))
                    )
                )
            }
        } finally {
            cursor.close()
            db.close()
        }
        return lista
    }

    // =============================================
    // ACCIONES SOBRE LA COMPRA
    // =============================================

    /**
     * Marca la orden como RECIBIDA y, por cada línea, registra una ENTRADA en el
     * kárdex de Inventario (suma stock + deja historial + actualiza precio pactado
     * del proveedor). Solo aplica si la orden estaba PENDIENTE.
     */
    /**
     * Marca la orden como RECIBIDA y, por cada línea, registra una ENTRADA en el
     * kárdex de Inventario (suma stock + deja historial + actualiza precio pactado
     * del proveedor). Solo aplica si la orden estaba PENDIENTE.
     *
     * Importante: el stock se actualiza ANTES de cambiar el estado de la compra.
     * Si alguna línea falla, la compra se queda en PENDIENTE (no en un estado
     * "RECIBIDA a medias") para poder reintentar sin haber perdido ni duplicado
     * información.
     */
    fun marcarRecibida(idCompra: Int, usuario: String): Boolean {
        val compra = obtenerCompra(idCompra) ?: return false
        if (compra.estado != "PENDIENTE") return false

        val detalles = listarDetalle(idCompra)
        if (detalles.isEmpty()) return false

        // 1. Primero se intenta sumar el stock de cada línea. La compra sigue
        //    en PENDIENTE mientras esto ocurre.
        for (detalle in detalles) {
            val ok = inventarioRepository.registrarEntrada(
                idProducto = detalle.idProducto,
                cantidad = detalle.cantidad,
                idProveedor = compra.idProveedor,
                precioUnitarioPactado = detalle.precioUnitario,
                notas = "Recepción de orden ${compra.codigo}",
                usuario = usuario
            )
            if (!ok) {
                // No se cambia el estado: la orden queda PENDIENTE para reintentar.
                return false
            }
        }

        // 2. Solo si TODAS las líneas sumaron stock correctamente, se marca
        //    la compra como RECIBIDA.
        val db = dbHelper.writableDatabase
        val filas = db.update(
            "compras",
            ContentValues().apply { put("estado", "RECIBIDA") },
            "id_compra = ? AND estado = 'PENDIENTE'",
            arrayOf(idCompra.toString())
        )
        db.close()
        return filas > 0
    }

    /** Anula la compra. Solo aplica si estaba PENDIENTE (una compra recibida ya afectó el stock). */
    fun anularCompra(idCompra: Int): Boolean {
        val db = dbHelper.writableDatabase
        val filas = db.update(
            "compras",
            ContentValues().apply { put("estado", "ANULADA") },
            "id_compra = ? AND estado = 'PENDIENTE'",
            arrayOf(idCompra.toString())
        )
        db.close()
        return filas > 0
    }

    // =============================================
    // PAGOS A PROVEEDOR
    // =============================================

    fun listarPagos(idCompra: Int): List<PagoCompra> {
        val lista = mutableListOf<PagoCompra>()
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            "pagos_compra", null, "id_compra = ?", arrayOf(idCompra.toString()),
            null, null, "id_pago_compra ASC"
        )
        try {
            while (cursor.moveToNext()) {
                lista.add(
                    PagoCompra(
                        idPagoCompra = cursor.getInt(cursor.getColumnIndexOrThrow("id_pago_compra")),
                        fecha = cursor.getString(cursor.getColumnIndexOrThrow("fecha")),
                        metodoPago = cursor.getString(cursor.getColumnIndexOrThrow("metodo_pago")),
                        monto = cursor.getDouble(cursor.getColumnIndexOrThrow("monto")),
                        efectivoRecibido = if (cursor.isNull(cursor.getColumnIndexOrThrow("efectivo_recibido"))) null
                        else cursor.getDouble(cursor.getColumnIndexOrThrow("efectivo_recibido")),
                        observacion = cursor.getString(cursor.getColumnIndexOrThrow("observacion")),
                        idCompra = cursor.getInt(cursor.getColumnIndexOrThrow("id_compra")),
                        registradoPor = cursor.getString(cursor.getColumnIndexOrThrow("registrado_por"))
                    )
                )
            }
        } finally {
            cursor.close()
            db.close()
        }
        return lista
    }

    /** Registra un abono. No permite abonar más del saldo pendiente ni montos <= 0. Devuelve -1 si no es válido. */
    fun registrarPago(pago: PagoCompra): Long {
        val compra = obtenerCompra(pago.idCompra) ?: return -1L
        val saldo = compra.saldoPendiente
        if (pago.monto <= 0 || pago.monto > saldo + 0.009) return -1L

        val db = dbHelper.writableDatabase
        val valores = ContentValues().apply {
            put("fecha", pago.fecha)
            put("metodo_pago", pago.metodoPago)
            put("monto", pago.monto)
            put("efectivo_recibido", pago.efectivoRecibido)
            put("observacion", pago.observacion)
            put("registrado_por", pago.registradoPor)
            put("id_compra", pago.idCompra)
        }
        val id = db.insert("pagos_compra", null, valores)
        db.close()
        return id
    }

    // =============================================
    // ESTADÍSTICAS DEL DASHBOARD DE REABASTECIMIENTO
    // =============================================
    fun obtenerEstadisticas(): Estadisticas {
        val db = dbHelper.readableDatabase

        var ordenesAbiertas = 0
        db.rawQuery("SELECT COUNT(*) FROM compras WHERE estado = 'PENDIENTE'", null).use {
            if (it.moveToFirst()) ordenesAbiertas = it.getInt(0)
        }

        var pendientePago = 0.0
        db.rawQuery(
            """
            SELECT COALESCE(SUM(co.total - COALESCE(pg.pagado, 0)), 0)
            FROM compras co
            LEFT JOIN (
                SELECT id_compra, SUM(monto) AS pagado FROM pagos_compra GROUP BY id_compra
            ) pg ON pg.id_compra = co.id_compra
            WHERE co.estado != 'ANULADA'
            """.trimIndent(), null
        ).use {
            if (it.moveToFirst()) pendientePago = it.getDouble(0)
        }

        var totalRecibido = 0.0
        var pedidosRecibidos = 0
        db.rawQuery(
            "SELECT COALESCE(SUM(total), 0), COUNT(*) FROM compras WHERE estado = 'RECIBIDA'", null
        ).use {
            if (it.moveToFirst()) {
                totalRecibido = it.getDouble(0)
                pedidosRecibidos = it.getInt(1)
            }
        }

        db.close()
        return Estadisticas(ordenesAbiertas, pendientePago, totalRecibido, pedidosRecibidos)
    }
}1234