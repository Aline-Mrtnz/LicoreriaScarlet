package com.example.scarlet.data.repository

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.example.scarlet.database.databasehelpers
import com.example.scarlet.util.FechaUtils

data class TurnoCaja(
    val idTurno: Int,
    val fechaApertura: String,
    val montoApertura: Double,
    val fechaCierre: String?,
    val montoCierreContado: Double?,
    val montoCierreEsperado: Double?,
    val diferencia: Double?,
    val estado: String,
    val observacion: String?,
    val idCuenta: Int
)

sealed class ResultadoTurno {
    data class Exito(val idTurno: Long) : ResultadoTurno()
    object YaHayTurnoAbierto : ResultadoTurno()
    object NoHayTurnoAbierto : ResultadoTurno()
    data class Error(val mensaje: String) : ResultadoTurno()
}

class TurnoCajaRepository(context: Context) {

    private val dbHelper = databasehelpers(context)

    /** Turno abierto de esta cuenta. Cada Cajero abre/cierra su propia caja. */
    fun turnoAbierto(idCuenta: Int): TurnoCaja? {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM turnos_caja WHERE id_cuenta = ? AND estado = 'ABIERTO' LIMIT 1",
            arrayOf(idCuenta.toString())
        )
        val turno = if (cursor.moveToFirst()) mapear(cursor) else null
        cursor.close()
        db.close()
        return turno
    }

    fun abrirTurno(idCuenta: Int, montoApertura: Double): ResultadoTurno {
        if (turnoAbierto(idCuenta) != null) return ResultadoTurno.YaHayTurnoAbierto

        val db = dbHelper.writableDatabase
        return try {
            val id = db.insert("turnos_caja", null, ContentValues().apply {
                put("fecha_apertura", FechaUtils.ahora())
                put("monto_apertura", montoApertura)
                put("estado", "ABIERTO")
                put("id_cuenta", idCuenta)
            })
            if (id > 0) ResultadoTurno.Exito(id) else ResultadoTurno.Error("No se pudo abrir la caja")
        } finally {
            db.close()
        }
    }

    /**
     * Cierra el turno abierto. El "esperado" = apertura + ventas en Efectivo
     * registradas por esa cuenta desde que abrió la caja. La diferencia
     * (contado - esperado) queda guardada para el arqueo.
     */
    fun cerrarTurno(idCuenta: Int, montoContado: Double, observacion: String?): ResultadoTurno {
        val turno = turnoAbierto(idCuenta) ?: return ResultadoTurno.NoHayTurnoAbierto

        val db = dbHelper.writableDatabase
        return try {
            val ahora = FechaUtils.ahora()

            val cursor = db.rawQuery(
                """
                SELECT COALESCE(SUM(v.total), 0) AS total_efectivo
                FROM ventas v
                INNER JOIN pagos p ON v.id_pago = p.id_pago
                WHERE v.cuenta_id_cuenta = ?
                  AND p.tipo_pago = 'Efectivo'
                  AND v.fecha_venta BETWEEN ? AND ?
                """.trimIndent(),
                arrayOf(idCuenta.toString(), turno.fechaApertura, ahora)
            )
            var totalEfectivoVentas = 0.0
            if (cursor.moveToFirst()) {
                totalEfectivoVentas = cursor.getDouble(cursor.getColumnIndexOrThrow("total_efectivo"))
            }
            cursor.close()

            val esperado = turno.montoApertura + totalEfectivoVentas
            val diferencia = montoContado - esperado

            val filas = db.update("turnos_caja", ContentValues().apply {
                put("fecha_cierre", ahora)
                put("monto_cierre_contado", montoContado)
                put("monto_cierre_esperado", esperado)
                put("diferencia", diferencia)
                put("estado", "CERRADO")
                put("observacion", observacion)
            }, "id_turno = ?", arrayOf(turno.idTurno.toString()))

            if (filas > 0) ResultadoTurno.Exito(turno.idTurno.toLong())
            else ResultadoTurno.Error("No se pudo cerrar la caja")
        } finally {
            db.close()
        }
    }

    fun listarHistorial(idCuenta: Int? = null): List<TurnoCaja> {
        val db = dbHelper.readableDatabase
        val cursor = if (idCuenta != null)
            db.rawQuery("SELECT * FROM turnos_caja WHERE id_cuenta = ? ORDER BY fecha_apertura DESC", arrayOf(idCuenta.toString()))
        else
            db.rawQuery("SELECT * FROM turnos_caja ORDER BY fecha_apertura DESC", null)
        val lista = mutableListOf<TurnoCaja>()
        while (cursor.moveToNext()) lista.add(mapear(cursor))
        cursor.close()
        db.close()
        return lista
    }

    private fun mapear(cursor: Cursor): TurnoCaja = TurnoCaja(
        idTurno = cursor.getInt(cursor.getColumnIndexOrThrow("id_turno")),
        fechaApertura = cursor.getString(cursor.getColumnIndexOrThrow("fecha_apertura")),
        montoApertura = cursor.getDouble(cursor.getColumnIndexOrThrow("monto_apertura")),
        fechaCierre = cursor.getString(cursor.getColumnIndexOrThrow("fecha_cierre")),
        montoCierreContado = if (cursor.isNull(cursor.getColumnIndexOrThrow("monto_cierre_contado"))) null else cursor.getDouble(cursor.getColumnIndexOrThrow("monto_cierre_contado")),
        montoCierreEsperado = if (cursor.isNull(cursor.getColumnIndexOrThrow("monto_cierre_esperado"))) null else cursor.getDouble(cursor.getColumnIndexOrThrow("monto_cierre_esperado")),
        diferencia = if (cursor.isNull(cursor.getColumnIndexOrThrow("diferencia"))) null else cursor.getDouble(cursor.getColumnIndexOrThrow("diferencia")),
        estado = cursor.getString(cursor.getColumnIndexOrThrow("estado")),
        observacion = cursor.getString(cursor.getColumnIndexOrThrow("observacion")),
        idCuenta = cursor.getInt(cursor.getColumnIndexOrThrow("id_cuenta"))
    )
}