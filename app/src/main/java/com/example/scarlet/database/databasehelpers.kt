package com.example.scarlet.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.content.ContentValues

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(
        context,
        DATABASE_NAME,
        null,
        DATABASE_VERSION
    ) {

    companion object {
        private const val DATABASE_NAME = "LicoreriaScarlet.db"
        private const val DATABASE_VERSION = 1
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Crear tablas
        createTables(db)
        // Insertar datos iniciales (roles, persona y cuenta)
        insertInitialData(db)
    }

    private fun createTables(db: SQLiteDatabase) {
        // PERSONA
        db.execSQL(
            """
            CREATE TABLE persona (
                id_persona INTEGER PRIMARY KEY AUTOINCREMENT,
                nombres TEXT NOT NULL,
                apellidos TEXT NOT NULL,
                ci TEXT UNIQUE,
                telefono TEXT
            )
            """.trimIndent()
        )

        // ROLES
        db.execSQL(
            """
            CREATE TABLE roles (
                id_rol INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre_rol TEXT NOT NULL UNIQUE,
                descripcion_rol TEXT
            )
            """.trimIndent()
        )

        // CUENTA
        db.execSQL(
            """
            CREATE TABLE cuenta (
                id_cuenta INTEGER PRIMARY KEY AUTOINCREMENT,
                usuario TEXT NOT NULL UNIQUE,
                clave TEXT NOT NULL,
                estado_cuenta TEXT NOT NULL DEFAULT 'Activo',
                id_persona INTEGER NOT NULL,
                id_rol INTEGER NOT NULL,

                FOREIGN KEY (id_persona)
                    REFERENCES persona(id_persona)
                    ON UPDATE CASCADE
                    ON DELETE RESTRICT,

                FOREIGN KEY (id_rol)
                    REFERENCES roles(id_rol)
                    ON UPDATE CASCADE
                    ON DELETE RESTRICT
            )
            """.trimIndent()
        )
    }

    private fun insertInitialData(db: SQLiteDatabase) {
        // 1. Insertar roles
        insertRoles(db)

        // 2. Insertar persona
        val personId = insertPersona(db)

        // 3. Insertar cuenta para esa persona
        if (personId != -1L) {
            insertCuenta(db, personId)
        }
    }

    private fun insertRoles(db: SQLiteDatabase) {
        // Roles disponibles
        val roles = listOf(
            "Administrador" to "Acceso completo al sistema",
            "Vendedor" to "Acceso a ventas y productos",
            "Cliente" to "Acceso limitado como cliente"
        )

        roles.forEach { (nombre, descripcion) ->
            val values = ContentValues().apply {
                put("nombre_rol", nombre)
                put("descripcion_rol", descripcion)
            }
            db.insert("roles", null, values)
        }
    }

    private fun insertPersona(db: SQLiteDatabase): Long {
        val personaValues = ContentValues().apply {
            put("nombres", "Scarlet")
            put("apellidos", "Liquor")
            put("ci", "12345678")
            put("telefono", "600112233")
        }

        return db.insert("persona", null, personaValues)
    }

    private fun insertCuenta(db: SQLiteDatabase, personId: Long) {
        // Obtener el ID del rol Administrador (id_rol = 1)
        val rolId = 1L // Administrador

        val cuentaValues = ContentValues().apply {
            put("usuario", "Scarlet")
            put("clave", "1234")
            put("estado_cuenta", "Activo")
            put("id_persona", personId)
            put("id_rol", rolId)
        }

        db.insert("cuenta", null, cuentaValues)
    }
    fun validarPin(pin: String): Boolean {
        val db = this.readableDatabase
        val cursor = db.rawQuery(
            "SELECT id_cuenta FROM cuenta WHERE clave = ? AND estado_cuenta = 'Activo'",
            arrayOf(pin)
        )
        val esValido = cursor.count > 0
        cursor.close()
        db.close()
        return esValido
    }

    override fun onUpgrade(
        db: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int
    ) {
        db.execSQL("DROP TABLE IF EXISTS cuenta")
        db.execSQL("DROP TABLE IF EXISTS roles")
        db.execSQL("DROP TABLE IF EXISTS persona")
        onCreate(db)
    }
}