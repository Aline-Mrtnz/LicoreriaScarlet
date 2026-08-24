package com.example.scarlet.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.database.sqlite.SQLiteException
import android.util.Log

class databasehelpers(context: Context) :
    SQLiteOpenHelper(context, "LicoreriaScarlet.db", null, 1) {

    companion object {
        private const val TAG = "DatabaseHelper"
    }

    override fun onCreate(db: SQLiteDatabase) {
        try {
            // ---------- Tablas sin dependencias ----------
            db.execSQL(
                """
                CREATE TABLE persona (
                    id_persona INTEGER PRIMARY KEY AUTOINCREMENT,
                    nombres TEXT NOT NULL,
                    apellidos TEXT NOT NULL,
                    ci TEXT,
                    telefono TEXT
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE roles (
                    id_rol INTEGER PRIMARY KEY AUTOINCREMENT,
                    nombre_rol TEXT NOT NULL,
                    descripcion TEXT
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE categorias (
                    id_categoria INTEGER PRIMARY KEY AUTOINCREMENT,
                    nombre_categoria TEXT NOT NULL,
                    descripcion TEXT
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE marcas (
                    id_marca INTEGER PRIMARY KEY AUTOINCREMENT,
                    nombre_marca TEXT NOT NULL,
                    descripcion_marca TEXT
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE pagos (
                    id_pago INTEGER PRIMARY KEY AUTOINCREMENT,
                    tipo_pago TEXT NOT NULL
                )
                """.trimIndent()
            )

            // ---------- Tablas con dependencias de primer nivel ----------
            db.execSQL(
                """
                CREATE TABLE cuenta (
                    id_cuenta INTEGER PRIMARY KEY AUTOINCREMENT,
                    usuario TEXT NOT NULL,
                    clave TEXT NOT NULL,
                    estado TEXT NOT NULL,
                    id_persona INTEGER NOT NULL,
                    id_rol INTEGER NOT NULL,
                    FOREIGN KEY (id_persona) REFERENCES persona(id_persona),
                    FOREIGN KEY (id_rol) REFERENCES roles(id_rol)
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE productos (
                    id_producto INTEGER PRIMARY KEY AUTOINCREMENT,
                    nombre_producto TEXT NOT NULL,
                    descripcion TEXT,
                    imagen TEXT,
                    precio_venta DECIMAL(10,2) NOT NULL,
                    precio_mayor DECIMAL(10,2),
                    stock INTEGER NOT NULL,
                    estado TEXT NOT NULL,
                    id_categoria INTEGER NOT NULL,
                    marcas_id_marca INTEGER NOT NULL,
                    FOREIGN KEY (id_categoria) REFERENCES categorias(id_categoria),
                    FOREIGN KEY (marcas_id_marca) REFERENCES marcas(id_marca)
                )
                """.trimIndent()
            )

            // ---------- Tablas con dependencias de segundo nivel ----------
            db.execSQL(
                """
                CREATE TABLE reportes (
                    id_reporte INTEGER PRIMARY KEY AUTOINCREMENT,
                    tipo_reporte TEXT NOT NULL,
                    descripcion TEXT,
                    fecha_generacion DATETIME NOT NULL,
                    cuenta_id_cuenta INTEGER NOT NULL,
                    FOREIGN KEY (cuenta_id_cuenta) REFERENCES cuenta(id_cuenta)
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE ventas (
                    id_venta INTEGER PRIMARY KEY AUTOINCREMENT,
                    fecha_venta DATETIME NOT NULL,
                    total DECIMAL(10,2) NOT NULL,
                    id_cliente INTEGER NOT NULL,
                    id_pago INTEGER NOT NULL,
                    cuenta_id_cuenta INTEGER NOT NULL,
                    FOREIGN KEY (id_cliente) REFERENCES persona(id_persona),
                    FOREIGN KEY (id_pago) REFERENCES pagos(id_pago),
                    FOREIGN KEY (cuenta_id_cuenta) REFERENCES cuenta(id_cuenta)
                )
                """.trimIndent()
            )

            // ---------- Tablas con dependencias de tercer nivel ----------
            db.execSQL(
                """
                CREATE TABLE detalle_venta (
                    id_detalle_venta INTEGER PRIMARY KEY AUTOINCREMENT,
                    cantidad INTEGER NOT NULL,
                    precio DECIMAL(10,2) NOT NULL,
                    id_venta INTEGER NOT NULL,
                    id_producto INTEGER NOT NULL,
                    FOREIGN KEY (id_venta) REFERENCES ventas(id_venta),
                    FOREIGN KEY (id_producto) REFERENCES productos(id_producto)
                )
                """.trimIndent()
            )

            // ---------- Datos por defecto ----------
            insertarDatosPorDefecto(db)

            Log.d(TAG, "Base de datos creada exitosamente")
        } catch (e: Exception) {
            Log.e(TAG, "Error al crear la base de datos: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    private fun insertarDatosPorDefecto(db: SQLiteDatabase) {
        try {
            // Verificar si ya existen datos
            val cursor = db.query("roles", arrayOf("id_rol"), null, null, null, null, null)
            if (cursor.count > 0) {
                cursor.close()
                return // Ya hay datos, no insertar nuevamente
            }
            cursor.close()

            // Rol por defecto
            val valoresRol = ContentValues().apply {
                put("nombre_rol", "Administrador")
                put("descripcion", "Rol con acceso total al sistema")
            }
            val idRol = db.insert("roles", null, valoresRol)

            // Persona por defecto
            val valoresPersona = ContentValues().apply {
                put("nombres", "Admin")
                put("apellidos", "Sistema")
                put("ci", "0000000")
                put("telefono", "00000000")
            }
            val idPersona = db.insert("persona", null, valoresPersona)

            // Cuenta por defecto (usuario: admin, PIN: 1234)
            val valoresCuenta = ContentValues().apply {
                put("usuario", "admin")
                put("clave", "1234")
                put("estado", "ACTIVO")
                put("id_persona", idPersona)
                put("id_rol", idRol)
            }
            db.insert("cuenta", null, valoresCuenta)

            Log.d(TAG, "Datos por defecto insertados")
        } catch (e: Exception) {
            Log.e(TAG, "Error al insertar datos por defecto: ${e.message}")
            e.printStackTrace()
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Se eliminan en orden inverso a la creación para respetar las llaves foráneas
        db.execSQL("DROP TABLE IF EXISTS detalle_venta")
        db.execSQL("DROP TABLE IF EXISTS ventas")
        db.execSQL("DROP TABLE IF EXISTS reportes")
        db.execSQL("DROP TABLE IF EXISTS productos")
        db.execSQL("DROP TABLE IF EXISTS cuenta")
        db.execSQL("DROP TABLE IF EXISTS pagos")
        db.execSQL("DROP TABLE IF EXISTS marcas")
        db.execSQL("DROP TABLE IF EXISTS categorias")
        db.execSQL("DROP TABLE IF EXISTS roles")
        db.execSQL("DROP TABLE IF EXISTS persona")
        onCreate(db)
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        try {
            db.setForeignKeyConstraintsEnabled(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error al configurar foreign keys: ${e.message}")
        }
    }

    fun validarPin(pin: String): Boolean {
        var db: SQLiteDatabase? = null
        var cursor: android.database.Cursor? = null
        try {
            db = readableDatabase
            cursor = db.query(
                "cuenta",
                arrayOf("id_cuenta"),
                "clave = ? AND estado = ?",
                arrayOf(pin, "ACTIVO"),
                null, null, null
            )
            val esValido = cursor.count > 0
            return esValido
        } catch (e: SQLiteException) {
            Log.e(TAG, "Error SQLite al validar PIN: ${e.message}")
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error al validar PIN: ${e.message}")
            throw e
        } finally {
            cursor?.close()
            db?.close()
        }
    }
}