// app/src/main/java/com/example/scarlet/data/repository/ProductosRepository.kt

package com.example.scarlet.data.repository

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.example.scarlet.data.model.Producto
import com.example.scarlet.database.databasehelpers

class ProductosRepository(private val context: Context) {

    private val dbHelper = databasehelpers(context)

    /**
     * Obtiene todos los productos activos
     */
    fun obtenerTodosLosProductos(): List<Producto> {
        val productos = mutableListOf<Producto>()
        val db = dbHelper.readableDatabase

        val query = """
            SELECT 
                p.id_producto,
                p.nombre_producto,
                p.descripcion,
                p.imagen,
                p.precio_venta,
                p.precio_mayor,
                p.precio_compra,
                p.stock,
                p.stock_minimo,
                p.estado,
                p.id_categoria,
                p.marcas_id_marca,
                COALESCE(c.nombre_categoria, 'Sin categoría') AS nombre_categoria,
                COALESCE(m.nombre_marca, 'Sin marca') AS nombre_marca
            FROM productos p
            LEFT JOIN categorias c ON p.id_categoria = c.id_categoria
            LEFT JOIN marcas m ON p.marcas_id_marca = m.id_marca
            WHERE p.estado = 'ACTIVO'
            ORDER BY p.id_producto DESC
        """.trimIndent()

        val cursor = db.rawQuery(query, null)

        try {
            while (cursor.moveToNext()) {
                productos.add(extraerProductoDeCursor(cursor))
            }
        } finally {
            cursor.close()
            db.close()
        }

        return productos
    }

    /**
     * Obtiene productos con límite (para la pantalla principal)
     */
    fun obtenerProductosRecientes(limite: Int = 10): List<Producto> {
        val productos = mutableListOf<Producto>()
        val db = dbHelper.readableDatabase

        val query = """
            SELECT 
                p.id_producto,
                p.nombre_producto,
                p.descripcion,
                p.imagen,
                p.precio_venta,
                p.precio_mayor,
                p.precio_compra,
                p.stock,
                p.stock_minimo,
                p.estado,
                p.id_categoria,
                p.marcas_id_marca,
                COALESCE(c.nombre_categoria, 'Sin categoría') AS nombre_categoria,
                COALESCE(m.nombre_marca, 'Sin marca') AS nombre_marca
            FROM productos p
            LEFT JOIN categorias c ON p.id_categoria = c.id_categoria
            LEFT JOIN marcas m ON p.marcas_id_marca = m.id_marca
            WHERE p.estado = 'ACTIVO'
            ORDER BY p.id_producto DESC
            LIMIT ?
        """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(limite.toString()))

        try {
            while (cursor.moveToNext()) {
                productos.add(extraerProductoDeCursor(cursor))
            }
        } finally {
            cursor.close()
            db.close()
        }

        return productos
    }

    /**
     * Obtiene un producto por su ID
     */
    fun obtenerProductoPorId(idProducto: Int): Producto? {
        var producto: Producto? = null
        val db = dbHelper.readableDatabase

        val query = """
            SELECT 
                p.id_producto,
                p.nombre_producto,
                p.descripcion,
                p.imagen,
                p.precio_venta,
                p.precio_mayor,
                p.precio_compra,
                p.stock,
                p.stock_minimo,
                p.estado,
                p.id_categoria,
                p.marcas_id_marca,
                COALESCE(c.nombre_categoria, 'Sin categoría') AS nombre_categoria,
                COALESCE(m.nombre_marca, 'Sin marca') AS nombre_marca
            FROM productos p
            LEFT JOIN categorias c ON p.id_categoria = c.id_categoria
            LEFT JOIN marcas m ON p.marcas_id_marca = m.id_marca
            WHERE p.id_producto = ? AND p.estado = 'ACTIVO'
        """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(idProducto.toString()))

        try {
            if (cursor.moveToFirst()) {
                producto = extraerProductoDeCursor(cursor)
            }
        } finally {
            cursor.close()
            db.close()
        }

        return producto
    }

    /**
     * Obtiene productos por categoría
     */
    fun obtenerProductosPorCategoria(nombreCategoria: String): List<Producto> {
        val productos = mutableListOf<Producto>()
        val db = dbHelper.readableDatabase

        val query = """
            SELECT 
                p.id_producto,
                p.nombre_producto,
                p.descripcion,
                p.imagen,
                p.precio_venta,
                p.precio_mayor,
                p.precio_compra,
                p.stock,
                p.stock_minimo,
                p.estado,
                p.id_categoria,
                p.marcas_id_marca,
                COALESCE(c.nombre_categoria, 'Sin categoría') AS nombre_categoria,
                COALESCE(m.nombre_marca, 'Sin marca') AS nombre_marca
            FROM productos p
            LEFT JOIN categorias c ON p.id_categoria = c.id_categoria
            LEFT JOIN marcas m ON p.marcas_id_marca = m.id_marca
            WHERE p.estado = 'ACTIVO' AND c.nombre_categoria = ?
            ORDER BY p.nombre_producto ASC
        """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(nombreCategoria))

        try {
            while (cursor.moveToNext()) {
                productos.add(extraerProductoDeCursor(cursor))
            }
        } finally {
            cursor.close()
            db.close()
        }

        return productos
    }

    /**
     * Obtiene productos por marca
     */
    fun obtenerProductosPorMarca(nombreMarca: String): List<Producto> {
        val productos = mutableListOf<Producto>()
        val db = dbHelper.readableDatabase

        val query = """
            SELECT 
                p.id_producto,
                p.nombre_producto,
                p.descripcion,
                p.imagen,
                p.precio_venta,
                p.precio_mayor,
                p.precio_compra,
                p.stock,
                p.stock_minimo,
                p.estado,
                p.id_categoria,
                p.marcas_id_marca,
                COALESCE(c.nombre_categoria, 'Sin categoría') AS nombre_categoria,
                COALESCE(m.nombre_marca, 'Sin marca') AS nombre_marca
            FROM productos p
            LEFT JOIN categorias c ON p.id_categoria = c.id_categoria
            LEFT JOIN marcas m ON p.marcas_id_marca = m.id_marca
            WHERE p.estado = 'ACTIVO' AND m.nombre_marca = ?
            ORDER BY p.nombre_producto ASC
        """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(nombreMarca))

        try {
            while (cursor.moveToNext()) {
                productos.add(extraerProductoDeCursor(cursor))
            }
        } finally {
            cursor.close()
            db.close()
        }

        return productos
    }

    /**
     * Busca productos por nombre, categoría o marca
     */
    fun buscarProductos(query: String): List<Producto> {
        val productos = mutableListOf<Producto>()
        val db = dbHelper.readableDatabase

        val searchPattern = "%$query%"

        val sqlQuery = """
            SELECT 
                p.id_producto,
                p.nombre_producto,
                p.descripcion,
                p.imagen,
                p.precio_venta,
                p.precio_mayor,
                p.precio_compra,
                p.stock,
                p.stock_minimo,
                p.estado,
                p.id_categoria,
                p.marcas_id_marca,
                COALESCE(c.nombre_categoria, 'Sin categoría') AS nombre_categoria,
                COALESCE(m.nombre_marca, 'Sin marca') AS nombre_marca
            FROM productos p
            LEFT JOIN categorias c ON p.id_categoria = c.id_categoria
            LEFT JOIN marcas m ON p.marcas_id_marca = m.id_marca
            WHERE p.estado = 'ACTIVO' 
            AND (
                p.nombre_producto LIKE ? OR 
                c.nombre_categoria LIKE ? OR 
                m.nombre_marca LIKE ?
            )
            ORDER BY 
                CASE 
                    WHEN p.nombre_producto LIKE ? THEN 1
                    WHEN m.nombre_marca LIKE ? THEN 2
                    WHEN c.nombre_categoria LIKE ? THEN 3
                    ELSE 4
                END,
                p.nombre_producto ASC
        """.trimIndent()

        val cursor = db.rawQuery(
            sqlQuery,
            arrayOf(searchPattern, searchPattern, searchPattern, searchPattern, searchPattern, searchPattern)
        )

        try {
            while (cursor.moveToNext()) {
                productos.add(extraerProductoDeCursor(cursor))
            }
        } finally {
            cursor.close()
            db.close()
        }

        return productos
    }

    /**
     * Obtiene productos con stock bajo (stock <= stock_minimo)
     */
    fun obtenerProductosConStockBajo(): List<Producto> {
        val productos = mutableListOf<Producto>()
        val db = dbHelper.readableDatabase

        val query = """
            SELECT 
                p.id_producto,
                p.nombre_producto,
                p.descripcion,
                p.imagen,
                p.precio_venta,
                p.precio_mayor,
                p.precio_compra,
                p.stock,
                p.stock_minimo,
                p.estado,
                p.id_categoria,
                p.marcas_id_marca,
                COALESCE(c.nombre_categoria, 'Sin categoría') AS nombre_categoria,
                COALESCE(m.nombre_marca, 'Sin marca') AS nombre_marca
            FROM productos p
            LEFT JOIN categorias c ON p.id_categoria = c.id_categoria
            LEFT JOIN marcas m ON p.marcas_id_marca = m.id_marca
            WHERE p.estado = 'ACTIVO' AND p.stock <= p.stock_minimo
            ORDER BY p.stock ASC
        """.trimIndent()

        val cursor = db.rawQuery(query, null)

        try {
            while (cursor.moveToNext()) {
                productos.add(extraerProductoDeCursor(cursor))
            }
        } finally {
            cursor.close()
            db.close()
        }

        return productos
    }

    /**
     * Obtiene estadísticas de productos
     */
    fun obtenerEstadisticasProductos(): Map<String, Int> {
        val estadisticas = mutableMapOf<String, Int>()
        val db = dbHelper.readableDatabase

        // Total de productos activos
        val totalQuery = "SELECT COUNT(*) FROM productos WHERE estado = 'ACTIVO'"
        var cursor = db.rawQuery(totalQuery, null)
        try {
            if (cursor.moveToFirst()) {
                estadisticas["total"] = cursor.getInt(0)
            }
        } finally {
            cursor.close()
        }

        // Productos con stock bajo
        val stockBajoQuery = "SELECT COUNT(*) FROM productos WHERE estado = 'ACTIVO' AND stock <= stock_minimo"
        cursor = db.rawQuery(stockBajoQuery, null)
        try {
            if (cursor.moveToFirst()) {
                estadisticas["stock_bajo"] = cursor.getInt(0)
            }
        } finally {
            cursor.close()
        }

        // Productos sin stock
        val sinStockQuery = "SELECT COUNT(*) FROM productos WHERE estado = 'ACTIVO' AND stock = 0"
        cursor = db.rawQuery(sinStockQuery, null)
        try {
            if (cursor.moveToFirst()) {
                estadisticas["sin_stock"] = cursor.getInt(0)
            }
        } finally {
            cursor.close()
        }

        db.close()
        return estadisticas
    }

    /**
     * Actualiza el stock de un producto
     */
    fun actualizarStock(idProducto: Int, nuevoStock: Int): Boolean {
        val db = dbHelper.writableDatabase
        try {
            val values = android.content.ContentValues().apply {
                put("stock", nuevoStock)
            }
            val filasAfectadas = db.update(
                "productos",
                values,
                "id_producto = ?",
                arrayOf(idProducto.toString())
            )
            return filasAfectadas > 0
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        } finally {
            db.close()
        }
    }

    /**
     * Reduce el stock de un producto
     */
    fun reducirStock(idProducto: Int, cantidad: Int): Boolean {
        val db = dbHelper.writableDatabase
        try {
            // Obtener stock actual
            val cursor = db.query(
                "productos",
                arrayOf("stock"),
                "id_producto = ?",
                arrayOf(idProducto.toString()),
                null,
                null,
                null
            )
            var stockActual = 0
            if (cursor.moveToFirst()) {
                stockActual = cursor.getInt(0)
            }
            cursor.close()

            if (stockActual < cantidad) {
                return false // Stock insuficiente
            }

            val nuevoStock = stockActual - cantidad
            val values = android.content.ContentValues().apply {
                put("stock", nuevoStock)
            }
            val filasAfectadas = db.update(
                "productos",
                values,
                "id_producto = ?",
                arrayOf(idProducto.toString())
            )
            return filasAfectadas > 0
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        } finally {
            db.close()
        }
    }

    /**
     * Extrae un objeto Producto del cursor
     */
    private fun extraerProductoDeCursor(cursor: Cursor): Producto {
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
            marcasIdMarca = cursor.getInt(cursor.getColumnIndexOrThrow("marcas_id_marca")),
            nombreCategoria = cursor.getString(cursor.getColumnIndexOrThrow("nombre_categoria")),
            nombreMarca = cursor.getString(cursor.getColumnIndexOrThrow("nombre_marca"))
        )
    }
}