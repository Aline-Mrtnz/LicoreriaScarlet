// app/src/main/java/com/example/scarlet/data/repository/InventarioRepository.kt

package com.example.scarlet.data.repository

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.example.scarlet.data.model.MovimientoInventario
import com.example.scarlet.data.model.Producto
import com.example.scarlet.database.databasehelpers

/**
 * Repositorio del módulo de Inventario (pantalla "Gestión de Inventario").
 *
 * Se apoya en las tablas ya existentes `productos` y `proveedores` /
 * `proveedor_productos` (creadas por ProductosRepository y
 * ProveedoresRepository respectivamente) y añade el kárdex de movimientos
 * de stock (`movimientos_inventario`).
 */
class InventarioRepository(private val context: Context) {

    private val dbHelper = databasehelpers(context)

    // =============================================
    // LISTADO DE PRODUCTOS PARA INVENTARIO
    // =============================================

    /**
     * A diferencia de ProductosRepository.obtenerTodosLosProductos(), esta
     * consulta incluye también los productos INACTIVOS: en la pantalla de
     * Inventario el switch "Activo" solo oculta el producto del catálogo de
     * ventas, pero debe seguir siendo visible aquí para poder reactivarlo.
     */
    fun obtenerProductosInventario(): List<Producto> {
        val productos = mutableListOf<Producto>()
        val db = dbHelper.readableDatabase

        val query = """
            SELECT 
                p.id_producto, p.nombre_producto, p.descripcion, p.imagen,
                p.precio_venta, p.precio_mayor, p.precio_compra,
                p.stock, p.stock_minimo, p.estado, p.id_categoria,
                p.marcas_id_marca, p.volumen_ml, p.abv,
                COALESCE(c.nombre_categoria, 'Sin categoría') AS nombre_categoria,
                COALESCE(m.nombre_marca, 'Sin marca') AS nombre_marca
            FROM productos p
            LEFT JOIN categorias c ON p.id_categoria = c.id_categoria
            LEFT JOIN marcas m ON p.marcas_id_marca = m.id_marca
            ORDER BY p.nombre_producto ASC
        """.trimIndent()

        val cursor = db.rawQuery(query, null)
        try {
            while (cursor.moveToNext()) {
                productos.add(extraerProducto(cursor))
            }
        } finally {
            cursor.close()
            db.close()
        }
        return productos
    }

    private fun extraerProducto(cursor: Cursor): Producto {
        val idxMarca = cursor.getColumnIndexOrThrow("marcas_id_marca")
        val idxVolumen = cursor.getColumnIndexOrThrow("volumen_ml")
        val idxAbv = cursor.getColumnIndexOrThrow("abv")
        return Producto(
            idProducto = cursor.getInt(cursor.getColumnIndexOrThrow("id_producto")),
            nombreProducto = cursor.getString(cursor.getColumnIndexOrThrow("nombre_producto")),
            descripcion = cursor.getString(cursor.getColumnIndexOrThrow("descripcion")),
            imagen = cursor.getString(cursor.getColumnIndexOrThrow("imagen")),
            precioVenta = cursor.getDouble(cursor.getColumnIndexOrThrow("precio_venta")),
            precioMayor = cursor.getDouble(cursor.getColumnIndexOrThrow("precio_mayor")),
            precioCompra = cursor.getDouble(cursor.getColumnIndexOrThrow("precio_compra")),
            stock = cursor.getInt(cursor.getColumnIndexOrThrow("stock")),
            stockMinimo = cursor.getInt(cursor.getColumnIndexOrThrow("stock_minimo")),
            estado = cursor.getString(cursor.getColumnIndexOrThrow("estado")),
            idCategoria = cursor.getInt(cursor.getColumnIndexOrThrow("id_categoria")),
            marcasIdMarca = if (cursor.isNull(idxMarca)) null else cursor.getInt(idxMarca),
            volumenMl = if (cursor.isNull(idxVolumen)) null else cursor.getInt(idxVolumen),
            abv = if (cursor.isNull(idxAbv)) null else cursor.getDouble(idxAbv),
            nombreCategoria = cursor.getString(cursor.getColumnIndexOrThrow("nombre_categoria")),
            nombreMarca = cursor.getString(cursor.getColumnIndexOrThrow("nombre_marca"))
        )
    }

    // =============================================
    // VALOR TOTAL DE INVENTARIO
    // =============================================

    /** Suma de (precio_venta * stock) de todos los productos activos. */
    fun obtenerValorTotalStock(): Double {
        val db = dbHelper.readableDatabase
        var total = 0.0
        val cursor = db.rawQuery(
            "SELECT COALESCE(SUM(precio_venta * stock), 0) FROM productos WHERE estado = 'ACTIVO'",
            null
        )
        try {
            if (cursor.moveToFirst()) total = cursor.getDouble(0)
        } finally {
            cursor.close()
            db.close()
        }
        return total
    }

    // =============================================
    // REABASTECER (ENTRADA DE STOCK)
    // =============================================

    /**
     * Registra una compra a proveedor: aumenta el stock del producto, deja
     * constancia en el kárdex y (si se indica un proveedor) actualiza el
     * precio pactado en proveedor_productos para futuras referencias.
     */
    fun registrarEntrada(
        idProducto: Int,
        cantidad: Int,
        idProveedor: Int?,
        precioUnitarioPactado: Double?,
        notas: String?,
        usuario: String
    ): Boolean {
        if (cantidad <= 0) return false
        val db = dbHelper.writableDatabase
        try {
            db.beginTransaction()

            val cursorStock = db.query(
                "productos", arrayOf("stock"), "id_producto = ?",
                arrayOf(idProducto.toString()), null, null, null
            )
            var stockActual = 0
            if (cursorStock.moveToFirst()) stockActual = cursorStock.getInt(0)
            cursorStock.close()

            val stockNuevo = stockActual + cantidad

            val valoresProducto = ContentValues().apply { put("stock", stockNuevo) }
            db.update("productos", valoresProducto, "id_producto = ?", arrayOf(idProducto.toString()))

            val valoresMovimiento = ContentValues().apply {
                put("id_producto", idProducto)
                put("tipo", "ENTRADA")
                put("cantidad", cantidad)
                put("stock_anterior", stockActual)
                put("stock_nuevo", stockNuevo)
                put("origen", "Compra")
                put("notas", notas)
                if (idProveedor != null) put("id_proveedor", idProveedor) else putNull("id_proveedor")
                put("usuario", usuario)
            }
            db.insert("movimientos_inventario", null, valoresMovimiento)

            // Actualiza (o crea) el precio pactado del proveedor para este producto
            if (idProveedor != null && precioUnitarioPactado != null && precioUnitarioPactado > 0) {
                val existente = db.query(
                    "proveedor_productos", arrayOf("id_proveedor_producto"),
                    "id_proveedor = ? AND id_producto = ?",
                    arrayOf(idProveedor.toString(), idProducto.toString()),
                    null, null, null
                )
                val yaExiste = existente.moveToFirst()
                existente.close()

                val valoresPP = ContentValues().apply { put("precio_pactado", precioUnitarioPactado) }
                if (yaExiste) {
                    db.update(
                        "proveedor_productos", valoresPP,
                        "id_proveedor = ? AND id_producto = ?",
                        arrayOf(idProveedor.toString(), idProducto.toString())
                    )
                } else {
                    valoresPP.put("id_proveedor", idProveedor)
                    valoresPP.put("id_producto", idProducto)
                    db.insert("proveedor_productos", null, valoresPP)
                }
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

    /**
     * Registra una salida manual de stock (ajuste, merma, reserva, etc.).
     * Devuelve false si no hay stock suficiente.
     */
    fun registrarSalida(
        idProducto: Int,
        cantidad: Int,
        origen: String,
        notas: String?,
        usuario: String
    ): Boolean {
        if (cantidad <= 0) return false
        val db = dbHelper.writableDatabase
        try {
            db.beginTransaction()

            val cursorStock = db.query(
                "productos", arrayOf("stock"), "id_producto = ?",
                arrayOf(idProducto.toString()), null, null, null
            )
            var stockActual = 0
            if (cursorStock.moveToFirst()) stockActual = cursorStock.getInt(0)
            cursorStock.close()

            if (stockActual < cantidad) return false

            val stockNuevo = stockActual - cantidad

            val valoresProducto = ContentValues().apply { put("stock", stockNuevo) }
            db.update("productos", valoresProducto, "id_producto = ?", arrayOf(idProducto.toString()))

            val valoresMovimiento = ContentValues().apply {
                put("id_producto", idProducto)
                put("tipo", "SALIDA")
                put("cantidad", cantidad)
                put("stock_anterior", stockActual)
                put("stock_nuevo", stockNuevo)
                put("origen", origen)
                put("notas", notas)
                put("usuario", usuario)
            }
            db.insert("movimientos_inventario", null, valoresMovimiento)

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

    // =============================================
    // HISTORIAL DE MOVIMIENTOS (KÁRDEX)
    // =============================================

    /**
     * Historial de movimientos de un producto, opcionalmente filtrado por
     * tipo ("ENTRADA"/"SALIDA"/"AJUSTE"/null = todos).
     */
    fun obtenerMovimientos(idProducto: Int, tipo: String? = null): List<MovimientoInventario> {
        val lista = mutableListOf<MovimientoInventario>()
        val db = dbHelper.readableDatabase

        val condiciones = mutableListOf("m.id_producto = ?")
        val argumentos = mutableListOf(idProducto.toString())

        if (!tipo.isNullOrBlank() && tipo != "Todos los tipos") {
            condiciones.add("m.tipo = ?")
            argumentos.add(tipo)
        }

        val query = """
            SELECT 
                m.id_movimiento, m.id_producto, m.tipo, m.cantidad,
                m.stock_anterior, m.stock_nuevo, m.origen, m.notas,
                m.fecha, m.usuario, m.id_proveedor,
                p.nombre_producto,
                pr.razon_social AS nombre_proveedor
            FROM movimientos_inventario m
            LEFT JOIN productos p ON m.id_producto = p.id_producto
            LEFT JOIN proveedores pr ON m.id_proveedor = pr.id_proveedor
            WHERE ${condiciones.joinToString(" AND ")}
            ORDER BY m.fecha DESC, m.id_movimiento DESC
        """.trimIndent()

        val cursor = db.rawQuery(query, argumentos.toTypedArray())
        try {
            while (cursor.moveToNext()) {
                lista.add(extraerMovimiento(cursor))
            }
        } finally {
            cursor.close()
            db.close()
        }
        return lista
    }

    private fun extraerMovimiento(cursor: Cursor): MovimientoInventario {
        val idxProveedor = cursor.getColumnIndexOrThrow("id_proveedor")
        return MovimientoInventario(
            idMovimiento = cursor.getInt(cursor.getColumnIndexOrThrow("id_movimiento")),
            idProducto = cursor.getInt(cursor.getColumnIndexOrThrow("id_producto")),
            tipo = cursor.getString(cursor.getColumnIndexOrThrow("tipo")),
            cantidad = cursor.getInt(cursor.getColumnIndexOrThrow("cantidad")),
            stockAnterior = cursor.getInt(cursor.getColumnIndexOrThrow("stock_anterior")),
            stockNuevo = cursor.getInt(cursor.getColumnIndexOrThrow("stock_nuevo")),
            origen = cursor.getString(cursor.getColumnIndexOrThrow("origen")),
            notas = cursor.getString(cursor.getColumnIndexOrThrow("notas")),
            fecha = cursor.getString(cursor.getColumnIndexOrThrow("fecha")) ?: "",
            usuario = cursor.getString(cursor.getColumnIndexOrThrow("usuario")),
            idProveedor = if (cursor.isNull(idxProveedor)) null else cursor.getInt(idxProveedor),
            nombreProducto = cursor.getString(cursor.getColumnIndexOrThrow("nombre_producto")),
            nombreProveedor = cursor.getString(cursor.getColumnIndexOrThrow("nombre_proveedor"))
        )
    }

    // =============================================
    // PRECIO PACTADO CON PROVEEDOR (para autocompletar)
    // =============================================

    /** Precio pactado ya registrado entre un proveedor y un producto, si existe. */
    fun obtenerPrecioPactado(idProveedor: Int, idProducto: Int): Double? {
        val db = dbHelper.readableDatabase
        var precio: Double? = null
        val cursor = db.query(
            "proveedor_productos", arrayOf("precio_pactado"),
            "id_proveedor = ? AND id_producto = ?",
            arrayOf(idProveedor.toString(), idProducto.toString()),
            null, null, null
        )
        if (cursor.moveToFirst()) precio = cursor.getDouble(0)
        cursor.close()
        db.close()
        return precio
    }

    // =============================================
    // ESTADO ACTIVO / INACTIVO DE UN PRODUCTO
    // =============================================

    /** Cambia el estado ACTIVO/INACTIVO de un producto (switch "Activo" de la lista). */
    fun cambiarEstadoProducto(idProducto: Int, activo: Boolean): Boolean {
        val db = dbHelper.writableDatabase
        try {
            val values = ContentValues().apply {
                put("estado", if (activo) "ACTIVO" else "INACTIVO")
            }
            val filas = db.update("productos", values, "id_producto = ?", arrayOf(idProducto.toString()))
            return filas > 0
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        } finally {
            db.close()
        }
    }
}