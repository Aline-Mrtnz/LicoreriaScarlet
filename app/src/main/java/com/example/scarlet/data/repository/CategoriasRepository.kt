package com.example.scarlet.data.repository

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.example.scarlet.data.model.CategoriaConConteo
import com.example.scarlet.data.model.Categorias
import com.example.scarlet.database.databasehelpers

class CategoriasRepository(context: Context) {

    private val dbHelper = databasehelpers(context)

    companion object {
        const val ESTADO_ACTIVA = "ACTIVO"
        const val ESTADO_INACTIVA = "INACTIVO"
    }

    fun crear(categoria: Categorias): Long {
        val db = dbHelper.writableDatabase
        val valores = ContentValues().apply {
            put("nombre_categoria", categoria.nombre_categoria)
            put("descripcion", categoria.descripcion)
            put("imagen_referencia", categoria.imagen_referencia)
            put("etiquetas", categoria.etiquetas)
            put("destacado", if (categoria.destacado) 1 else 0)
            put("orden_menu", categoria.orden_menu)
            put("estado", categoria.estado)
        }
        val id = db.insert("categorias", null, valores)
        db.close()
        return id
    }

    fun editar(categoria: Categorias): Int {
        val db = dbHelper.writableDatabase
        val valores = ContentValues().apply {
            put("nombre_categoria", categoria.nombre_categoria)
            put("descripcion", categoria.descripcion)
            put("imagen_referencia", categoria.imagen_referencia)
            put("etiquetas", categoria.etiquetas)
            put("destacado", if (categoria.destacado) 1 else 0)
            put("orden_menu", categoria.orden_menu)
            put("estado", categoria.estado)
        }
        val filas = db.update("categorias", valores, "id_categoria = ?", arrayOf(categoria.id_categoria.toString()))
        db.close()
        return filas
    }

    /**
     * Cambia únicamente el estado (ACTIVO/INACTIVO) de una categoría,
     * usado por el switch de la lista de "Gestión de Categorías".
     */
    fun actualizarEstado(id_categoria: Int, nuevoEstado: String): Int {
        val db = dbHelper.writableDatabase
        val valores = ContentValues().apply { put("estado", nuevoEstado) }
        val filas = db.update("categorias", valores, "id_categoria = ?", arrayOf(id_categoria.toString()))
        db.close()
        return filas
    }

    /**
     * Elimina una categoría. Lanza SQLiteConstraintException si existen
     * productos que todavía la referencian (llave foránea); el llamador
     * debe validar con [contarProductos] antes de invocar este método
     * para poder mostrar un mensaje claro al usuario.
     */
    fun eliminar(id_categoria: Int): Int {
        val db = dbHelper.writableDatabase
        val filas = db.delete("categorias", "id_categoria = ?", arrayOf(id_categoria.toString()))
        db.close()
        return filas
    }

    fun obtenerPorId(id_categoria: Int): Categorias? {
        val db = dbHelper.readableDatabase
        val cursor = db.query("categorias", null, "id_categoria = ?", arrayOf(id_categoria.toString()), null, null, null)
        var categoria: Categorias? = null
        if (cursor.moveToFirst()) categoria = mapear(cursor)
        cursor.close()
        db.close()
        return categoria
    }

    /** Todas las categorías, sin importar su estado, ordenadas por orden_menu. */
    fun listar(): List<Categorias> {
        val lista = mutableListOf<Categorias>()
        val db = dbHelper.readableDatabase
        val cursor = db.query("categorias", null, null, null, null, null, "orden_menu ASC, nombre_categoria ASC")
        if (cursor.moveToFirst()) {
            do { lista.add(mapear(cursor)) } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return lista
    }

    /** Solo categorías ACTIVAS (para spinners al crear/editar productos). */
    fun listarActivas(): List<Categorias> {
        val lista = mutableListOf<Categorias>()
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            "categorias", null, "estado = ?", arrayOf(ESTADO_ACTIVA), null, null,
            "orden_menu ASC, nombre_categoria ASC"
        )
        if (cursor.moveToFirst()) {
            do { lista.add(mapear(cursor)) } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return lista
    }

    /** Cuántos productos (activos) usan actualmente una categoría. */
    fun contarProductos(id_categoria: Int): Int {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM productos WHERE id_categoria = ? AND estado = 'ACTIVO'",
            arrayOf(id_categoria.toString())
        )
        var total = 0
        if (cursor.moveToFirst()) total = cursor.getInt(0)
        cursor.close()
        db.close()
        return total
    }

    /**
     * Todas las categorías junto con la cantidad de productos que tienen.
     * Es la fuente de datos de la pantalla "Gestión de Categorías": ahí se
     * muestran TODAS (con o sin productos, activas o inactivas).
     */
    fun listarConConteo(): List<CategoriaConConteo> {
        val lista = mutableListOf<CategoriaConConteo>()
        val db = dbHelper.readableDatabase
        val query = """
            SELECT c.id_categoria, c.nombre_categoria, c.descripcion, c.imagen_referencia,
                   c.etiquetas, c.destacado, c.orden_menu, c.estado,
                   COUNT(p.id_producto) AS cantidad
            FROM categorias c
            LEFT JOIN productos p ON p.id_categoria = c.id_categoria AND p.estado = 'ACTIVO'
            GROUP BY c.id_categoria
            ORDER BY c.orden_menu ASC, c.nombre_categoria ASC
        """.trimIndent()
        val cursor = db.rawQuery(query, null)
        if (cursor.moveToFirst()) {
            do {
                lista.add(
                    CategoriaConConteo(
                        categoria = mapear(cursor),
                        cantidadProductos = cursor.getInt(cursor.getColumnIndexOrThrow("cantidad"))
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return lista
    }

    /**
     * Categorías que deben verse en el resto de la app (chips de Productos,
     * carruseles de inicio, etc): solo las ACTIVAS que ya tienen al menos
     * un producto registrado. Si una categoría nueva no tiene productos
     * todavía, no aparece aquí: solo es visible dentro de la pantalla
     * interna de Gestión de Categorías.
     */
    fun listarVisiblesEnCatalogo(): List<Categorias> {
        return listarConConteo()
            .filter { it.esVisibleEnCatalogo }
            .map { it.categoria }
    }

    /** Siguiente valor sugerido para "Orden en Menú" al crear una categoría nueva. */
    fun siguienteOrden(): Int {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT MAX(orden_menu) FROM categorias", null)
        var maximo = 0
        if (cursor.moveToFirst()) maximo = cursor.getInt(0)
        cursor.close()
        db.close()
        return maximo + 1
    }

    private fun mapear(cursor: Cursor): Categorias = Categorias(
        id_categoria = cursor.getInt(cursor.getColumnIndexOrThrow("id_categoria")),
        nombre_categoria = cursor.getString(cursor.getColumnIndexOrThrow("nombre_categoria")),
        descripcion = cursor.getString(cursor.getColumnIndexOrThrow("descripcion")) ?: "",
        imagen_referencia = cursor.getString(cursor.getColumnIndexOrThrow("imagen_referencia")),
        etiquetas = cursor.getString(cursor.getColumnIndexOrThrow("etiquetas")),
        destacado = cursor.getInt(cursor.getColumnIndexOrThrow("destacado")) == 1,
        orden_menu = cursor.getInt(cursor.getColumnIndexOrThrow("orden_menu")),
        estado = cursor.getString(cursor.getColumnIndexOrThrow("estado")) ?: ESTADO_ACTIVA
    )
}