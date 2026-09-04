package com.example.scarlet.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.database.sqlite.SQLiteException
import android.util.Log
import com.example.scarlet.util.PasswordUtils

class databasehelpers(context: Context) :
    SQLiteOpenHelper(context, "LicoreriaScarlet.db", null, 10) {  // Versión 10: añade turnos_caja (apertura/cierre y arqueo)
    companion object {
        private const val TAG = "DatabaseHelper"

        // Constantes para estados
        private const val ESTADO_ACTIVO = "ACTIVO"
        private const val ESTADO_INACTIVO = "INACTIVO"

        // Usuario por defecto
        private const val ADMIN_USUARIO = "admin"
        private const val ADMIN_CLAVE = "1234"
    }

    override fun onCreate(db: SQLiteDatabase) {
        try {
            crearTablas(db)
            insertarDatosPorDefecto(db)
            Log.d(TAG, "Base de datos creada exitosamente")
        } catch (e: Exception) {
            Log.e(TAG, "Error al crear la base de datos: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    // =============================================
    // CREACIÓN DE TABLAS
    // =============================================
    private fun crearTablas(db: SQLiteDatabase) {
        // ---------- Tablas sin dependencias ----------
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

        db.execSQL(
            """
            CREATE TABLE roles (
                id_rol INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre_rol TEXT NOT NULL UNIQUE,
                descripcion TEXT
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE categorias (
                id_categoria INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre_categoria TEXT NOT NULL UNIQUE,
                descripcion TEXT,
                imagen_referencia TEXT,
                etiquetas TEXT,
                destacado INTEGER NOT NULL DEFAULT 0,
                orden_menu INTEGER NOT NULL DEFAULT 0,
                estado TEXT NOT NULL DEFAULT 'ACTIVO'
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE marcas (
                id_marca INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre_marca TEXT NOT NULL UNIQUE,
                descripcion_marca TEXT,
                pais_origen TEXT,
                imagen_referencia TEXT
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE pagos (
                id_pago INTEGER PRIMARY KEY AUTOINCREMENT,
                tipo_pago TEXT NOT NULL UNIQUE,
                descripcion TEXT
            )
            """.trimIndent()
        )

        // ---------- Tablas con dependencias ----------
        db.execSQL(
            """
            CREATE TABLE cuenta (
                id_cuenta INTEGER PRIMARY KEY AUTOINCREMENT,
                usuario TEXT NOT NULL UNIQUE,
                clave TEXT NOT NULL,
                estado TEXT NOT NULL,
                fecha_creacion DATETIME DEFAULT CURRENT_TIMESTAMP,
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
                precio_compra DECIMAL(10,2),
                stock INTEGER NOT NULL,
                stock_minimo INTEGER DEFAULT 5,
                estado TEXT NOT NULL,
                id_categoria INTEGER NOT NULL,
                marcas_id_marca INTEGER,
                volumen_ml INTEGER,
                abv DECIMAL(5,2),
                FOREIGN KEY (id_categoria) REFERENCES categorias(id_categoria),
                FOREIGN KEY (marcas_id_marca) REFERENCES marcas(id_marca)
            )
            """.trimIndent()
        )

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
                descuento DECIMAL(10,2) DEFAULT 0,
                id_cliente INTEGER NOT NULL,
                id_pago INTEGER NOT NULL,
                cuenta_id_cuenta INTEGER NOT NULL,
                FOREIGN KEY (id_cliente) REFERENCES persona(id_persona),
                FOREIGN KEY (id_pago) REFERENCES pagos(id_pago),
                FOREIGN KEY (cuenta_id_cuenta) REFERENCES cuenta(id_cuenta)
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE detalle_venta (
                id_detalle_venta INTEGER PRIMARY KEY AUTOINCREMENT,
                cantidad INTEGER NOT NULL,
                precio_unitario DECIMAL(10,2) NOT NULL,
                subtotal DECIMAL(10,2) NOT NULL,
                id_venta INTEGER NOT NULL,
                id_producto INTEGER NOT NULL,
                FOREIGN KEY (id_venta) REFERENCES ventas(id_venta),
                FOREIGN KEY (id_producto) REFERENCES productos(id_producto)
            )
            """.trimIndent()
        )

        // ---------- Gestión de Proveedores ----------
        db.execSQL(
            """
            CREATE TABLE proveedores (
                id_proveedor INTEGER PRIMARY KEY AUTOINCREMENT,
                razon_social TEXT NOT NULL,
                rfc_nit TEXT,
                condicion_pago TEXT,
                marcas_asociadas TEXT,
                contacto_ejecutivo TEXT,
                telefono_contacto TEXT,
                estado TEXT NOT NULL DEFAULT 'ACTIVO',
                fecha_registro DATETIME DEFAULT CURRENT_TIMESTAMP
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE proveedor_productos (
                id_proveedor_producto INTEGER PRIMARY KEY AUTOINCREMENT,
                id_proveedor INTEGER NOT NULL,
                id_producto INTEGER NOT NULL,
                precio_pactado DECIMAL(10,2) NOT NULL,
                FOREIGN KEY (id_proveedor) REFERENCES proveedores(id_proveedor),
                FOREIGN KEY (id_producto) REFERENCES productos(id_producto),
                UNIQUE(id_proveedor, id_producto)
            )
            """.trimIndent()
        )

        // ---------- Módulo de Inventario: kárdex de movimientos ----------
        db.execSQL(
            """
            CREATE TABLE movimientos_inventario (
                id_movimiento INTEGER PRIMARY KEY AUTOINCREMENT,
                id_producto INTEGER NOT NULL,
                tipo TEXT NOT NULL,
                cantidad INTEGER NOT NULL,
                stock_anterior INTEGER NOT NULL,
                stock_nuevo INTEGER NOT NULL,
                origen TEXT,
                notas TEXT,
                fecha DATETIME DEFAULT CURRENT_TIMESTAMP,
                usuario TEXT,
                id_proveedor INTEGER,
                FOREIGN KEY (id_producto) REFERENCES productos(id_producto),
                FOREIGN KEY (id_proveedor) REFERENCES proveedores(id_proveedor)
            )
            """.trimIndent()
        )

        // ---------- Módulo de Reabastecimiento: órdenes de compra ----------
        db.execSQL(
            """
            CREATE TABLE compras (
                id_compra INTEGER PRIMARY KEY AUTOINCREMENT,
                codigo TEXT NOT NULL UNIQUE,
                fecha_emision DATETIME NOT NULL,
                observacion TEXT,
                estado TEXT NOT NULL DEFAULT 'PENDIENTE',
                total DECIMAL(10,2) NOT NULL DEFAULT 0,
                id_proveedor INTEGER NOT NULL,
                cuenta_id_cuenta INTEGER NOT NULL,
                FOREIGN KEY (id_proveedor) REFERENCES proveedores(id_proveedor),
                FOREIGN KEY (cuenta_id_cuenta) REFERENCES cuenta(id_cuenta)
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE detalle_compra (
                id_detalle_compra INTEGER PRIMARY KEY AUTOINCREMENT,
                cantidad INTEGER NOT NULL,
                precio_unitario DECIMAL(10,2) NOT NULL,
                subtotal DECIMAL(10,2) NOT NULL,
                id_compra INTEGER NOT NULL,
                id_producto INTEGER NOT NULL,
                FOREIGN KEY (id_compra) REFERENCES compras(id_compra),
                FOREIGN KEY (id_producto) REFERENCES productos(id_producto)
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE pagos_compra (
                id_pago_compra INTEGER PRIMARY KEY AUTOINCREMENT,
                fecha DATETIME NOT NULL,
                metodo_pago TEXT NOT NULL,
                monto DECIMAL(10,2) NOT NULL,
                efectivo_recibido DECIMAL(10,2),
                observacion TEXT,
                registrado_por TEXT,
                id_compra INTEGER NOT NULL,
                FOREIGN KEY (id_compra) REFERENCES compras(id_compra)
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE turnos_caja (
                id_turno INTEGER PRIMARY KEY AUTOINCREMENT,
                fecha_apertura DATETIME NOT NULL,
                monto_apertura DECIMAL(10,2) NOT NULL DEFAULT 0,
                fecha_cierre DATETIME,
                monto_cierre_contado DECIMAL(10,2),
                monto_cierre_esperado DECIMAL(10,2),
                diferencia DECIMAL(10,2),
                estado TEXT NOT NULL DEFAULT 'ABIERTO',
                observacion TEXT,
                id_cuenta INTEGER NOT NULL,
                FOREIGN KEY (id_cuenta) REFERENCES cuenta(id_cuenta)
            )
            """.trimIndent()
        )
    }

    // =============================================
    // DATOS POR DEFECTO (REALES)
    // =============================================
    private fun insertarDatosPorDefecto(db: SQLiteDatabase) {
        try {
            // Verificar si ya existen datos
            val cursor = db.query("roles", arrayOf("id_rol"), null, null, null, null, null)
            if (cursor.count > 0) {
                cursor.close()
                Log.d(TAG, "Los datos por defecto ya existen")
                return
            }
            cursor.close()

            db.beginTransaction()
            try {
                // 1. Insertar Roles
                val rolesIds = insertarRoles(db)

                // 2. Insertar Persona Admin
                val idPersonaAdmin = insertarPersonaAdmin(db)

                // 3. Insertar Cuenta Admin
                insertarCuentaAdmin(db, idPersonaAdmin, rolesIds)

                // 4. Insertar Vendedor
                insertarVendedor(db, rolesIds)

                // 5. Insertar Categorías Reales
                val categoriasIds = insertarCategoriasReales(db)

                // 6. Insertar Marcas Reales
                val marcasIds = insertarMarcasReales(db)

                // 7. Insertar Métodos de Pago
                insertarMetodosPago(db)

                // 8. Insertar Productos Reales
                insertarProductosReales(db, categoriasIds, marcasIds)

                // 9. Insertar Cliente Mostrador (venta rápida / walk-in)
                insertarClienteMostrador(db)

                db.setTransactionSuccessful()
                Log.d(TAG, "Datos por defecto insertados correctamente")
            } finally {
                db.endTransaction()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al insertar datos por defecto: ${e.message}")
            e.printStackTrace()
        }
    }

    // =============================================
    // INSERCIÓN DE ROLES
    // =============================================
    private fun insertarRoles(db: SQLiteDatabase): MutableMap<String, Long> {
        val rolesIds = mutableMapOf<String, Long>()

        val roles = listOf(
            "Administrador" to "Acceso total al sistema, gestión de usuarios y configuración",
            "Vendedor" to "Gestión de ventas, clientes y productos",
            "Almacenero" to "Gestión de inventario, productos y proveedores",
            "Cajero" to "Gestión de caja, ventas y pagos"
        )

        roles.forEach { (nombre, descripcion) ->
            val values = ContentValues().apply {
                put("nombre_rol", nombre)
                put("descripcion", descripcion)
            }
            val id = db.insert("roles", null, values)
            rolesIds[nombre] = id
        }

        return rolesIds
    }

    // =============================================
    // INSERCIÓN DE PERSONAS
    // =============================================
    private fun insertarPersonaAdmin(db: SQLiteDatabase): Long {
        val values = ContentValues().apply {
            put("nombres", "Juan Carlos")
            put("apellidos", "Pérez Rodríguez")
            put("ci", "123456789")
            put("telefono", "78945612")
        }
        return db.insert("persona", null, values)
    }

    // Cliente genérico usado para ventas de mostrador (walk-in) cuando no se
    // registra un cliente específico. Se identifica por su CI fijo "00000000"
    // para poder ubicarlo desde el resto de la app sin guardar su id en ningún
    // lado (ver PersonaRepository.obtenerClienteMostrador()).
    private fun insertarClienteMostrador(db: SQLiteDatabase): Long {
        val values = ContentValues().apply {
            put("nombres", "Cliente")
            put("apellidos", "Mostrador")
            put("ci", "00000000")
            put("telefono", "")
        }
        return db.insert("persona", null, values)
    }

    private fun insertarCuentaAdmin(db: SQLiteDatabase, idPersona: Long, rolesIds: Map<String, Long>) {
        val values = ContentValues().apply {
            put("usuario", ADMIN_USUARIO)
            //put("clave", ADMIN_CLAVE)
            put("clave", PasswordUtils.hash(ADMIN_CLAVE))
            put("estado", ESTADO_ACTIVO)
            put("id_persona", idPersona)
            put("id_rol", rolesIds["Administrador"] ?: 1)
        }
        db.insert("cuenta", null, values)
    }

    private fun insertarVendedor(db: SQLiteDatabase, rolesIds: Map<String, Long>) {
        // Persona Vendedor
        val valuesPersona = ContentValues().apply {
            put("nombres", "María Fernanda")
            put("apellidos", "López Gutiérrez")
            put("ci", "987654321")
            put("telefono", "65478912")
        }
        val idPersona = db.insert("persona", null, valuesPersona)

        // Cuenta Vendedor
        val valuesCuenta = ContentValues().apply {
            put("usuario", "vendedor")
            //put("clave", "5678")
            put("clave", PasswordUtils.hash("5678"))
            put("estado", ESTADO_ACTIVO)
            put("id_persona", idPersona)
            put("id_rol", rolesIds["Vendedor"] ?: 2)
        }
        db.insert("cuenta", null, valuesCuenta)
    }

    // =============================================
    // CATEGORÍAS REALES CON IMÁGENES
    // =============================================
    private fun insertarCategoriasReales(db: SQLiteDatabase): MutableMap<String, Long> {
        val categoriasIds = mutableMapOf<String, Long>()

        val categorias = listOf(
            Triple("Whisky", "Bebidas destiladas de alta calidad, añejadas en barricas de roble", "ic_whisky"),
            Triple("Tequila", "Bebidas destiladas elaboradas con agave azul de Jalisco", "ic_tequila"),
            Triple("Vino", "Bebidas fermentadas de uva, con variedad de cepas y añadas", "ic_vino"),
            Triple("Cerveza", "Bebidas fermentadas de cebada y lúpulo", "ic_cerveza"),
            Triple("Cognac", "Brandy de uva de la región de Cognac, Francia", "ic_cognac"),
            Triple("Vodka", "Bebida destilada neutra, de alta pureza", "ic_vodka"),
            Triple("Champagne", "Vino espumoso de la región de Champagne, Francia", "ic_champagne"),
            Triple("Ron", "Bebida destilada de caña de azúcar o melaza", "ic_ron"),
            Triple("Gin", "Bebida destilada con enebro y botánicos", "ic_gin"),
            Triple("Licores", "Bebidas destiladas con sabores dulces y frutales", "ic_licor")
        )

        categorias.forEachIndexed { index, (nombre, descripcion, imagen) ->
            val values = ContentValues().apply {
                put("nombre_categoria", nombre)
                put("descripcion", descripcion)
                put("imagen_referencia", imagen)
                put("etiquetas", "")
                put("destacado", 0)
                put("orden_menu", index + 1)
                put("estado", ESTADO_ACTIVO)
            }
            val id = db.insert("categorias", null, values)
            categoriasIds[nombre] = id
        }

        return categoriasIds
    }

    // =============================================
    // MARCAS REALES CON IMÁGENES
    // =============================================
    private fun insertarMarcasReales(db: SQLiteDatabase): MutableMap<String, Long> {
        val marcasIds = mutableMapOf<String, Long>()

        // CORRECCIÓN 1: Definir la lista correctamente
        // Opción A: Usando data class (RECOMENDADA)
        data class MarcaData(
            val nombre: String,
            val descripcion: String,
            val pais: String,
            val imagen: String
        )

        val marcas = listOf(
            MarcaData("Bacardi", "Ron puertorriqueño de fama mundial", "Puerto Rico", "ic_bacardi"),
            MarcaData("Havana Club", "Ron cubano auténtico", "Cuba", "ic_havana_club"),
            MarcaData("Zacapa", "Ron guatemalteco premium", "Guatemala", "ic_zacapa"),
            MarcaData("Bombay Sapphire", "Gin inglés con botánicos exóticos", "Inglaterra", "ic_bombay"),
            MarcaData("Tanqueray", "Gin inglés de alta calidad", "Inglaterra", "ic_tanqueray"),
            // NOTA: las marcas de abajo faltaban en el set original. Sin ellas,
            // TODOS los productos de Whisky, Tequila, Vino, Cognac, Vodka y
            // Champagne quedaban asignados por defecto a "Bacardi" (id 1), ya
            // que su marca real no existía en esta tabla.
            MarcaData("Macallan", "Whisky escocés de malta premium", "Escocia", "ic_macallan"),
            MarcaData("Johnnie Walker", "Whisky escocés blended de referencia mundial", "Escocia", "ic_johnnie_walker"),
            MarcaData("Jack Daniel's", "Whisky americano de Tennessee", "Estados Unidos", "ic_jack_daniels"),
            MarcaData("Jameson", "Whisky irlandés triple destilado", "Irlanda", "ic_jameson"),
            MarcaData("Clase Azul", "Tequila artesanal mexicano premium", "México", "ic_clase_azul"),
            MarcaData("Don Julio", "Tequila mexicano de alta gama", "México", "ic_don_julio"),
            MarcaData("Patrón", "Tequila mexicano ultra premium", "México", "ic_patron"),
            MarcaData("Casillero del Diablo", "Vino chileno de gran tradición", "Chile", "ic_casillero"),
            MarcaData("Concha y Toro", "Vino chileno, una de las bodegas más grandes del mundo", "Chile", "ic_concha_y_toro"),
            MarcaData("Santa Rita", "Vino chileno de larga tradición", "Chile", "ic_santa_rita"),
            MarcaData("Hennessy", "Cognac francés de referencia mundial", "Francia", "ic_hennessy"),
            MarcaData("Rémy Martin", "Cognac francés de alta gama", "Francia", "ic_remy_martin"),
            MarcaData("Grey Goose", "Vodka francés premium", "Francia", "ic_grey_goose"),
            MarcaData("Absolut", "Vodka sueco de fama mundial", "Suecia", "ic_absolut"),
            MarcaData("Belvedere", "Vodka polaco ultra premium", "Polonia", "ic_belvedere"),
            MarcaData("Veuve Clicquot", "Champagne francés de gran prestigio", "Francia", "ic_veuve_clicquot"),
            MarcaData("Moët & Chandon", "Champagne francés icónico", "Francia", "ic_moet")
        )

        // CORRECCIÓN 2: Iterar correctamente
        marcas.forEach { marca ->
            // CORRECCIÓN 3: Usar ContentValues correctamente (NO ContextValues)
            val values = ContentValues().apply {
                put("nombre_marca", marca.nombre)
                put("descripcion_marca", marca.descripcion)
                put("pais_origen", marca.pais)  // CORRECCIÓN: pais_origen (NO pais_origin)
                put("imagen_referencia", marca.imagen)
            }

            // CORRECCIÓN 4: Insertar correctamente (null en lugar de "nullColumnHack = null, values")
            val id = db.insert("marcas", null, values)
            marcasIds[marca.nombre] = id
        }

        return marcasIds
    }
    // =============================================
    // MÉTODOS DE PAGO
    // =============================================
    private fun insertarMetodosPago(db: SQLiteDatabase) {
        val pagos = listOf(
            "Efectivo" to "Pago en moneda física",
            "Tarjeta de Crédito" to "Pago con tarjeta de crédito",
            "Tarjeta de Débito" to "Pago con tarjeta de débito",
            "Transferencia" to "Transferencia bancaria",
            "QR" to "Pago con código QR"
        )

        pagos.forEach { (tipo, descripcion) ->
            val values = ContentValues().apply {
                put("tipo_pago", tipo)
                put("descripcion", descripcion)
            }
            db.insert("pagos", null, values)
        }
    }

    // =============================================
    // PRODUCTOS REALES CON IMÁGENES
    // =============================================
    private fun insertarProductosReales(
        db: SQLiteDatabase,
        categoriasIds: Map<String, Long>,
        marcasIds: Map<String, Long>
    ) {
        val productos = listOf(
            // WHISKY
            ProductoData(
                nombre = "Macallan 18 Years Sherry Oak",
                descripcion = "Whisky de malta escocés, añejado 18 años en barricas de roble de Jerez",
                imagen = "macallan_18",
                precioVenta = 349.00,
                precioMayor = 279.20,
                precioCompra = 210.00,
                stock = 12,
                stockMinimo = 3,
                categoria = "Whisky",
                marca = "Macallan"
            ),
            ProductoData(
                nombre = "Macallan 12 Years Double Cask",
                descripcion = "Whisky de malta con añejamiento en barricas de roble americano y europeo",
                imagen = "macallan_12",
                precioVenta = 120.00,
                precioMayor = 96.00,
                precioCompra = 72.00,
                stock = 20,
                stockMinimo = 5,
                categoria = "Whisky",
                marca = "Macallan"
            ),
            ProductoData(
                nombre = "Johnnie Walker Blue Label",
                descripcion = "Blended whisky escocés de lujo, con maltas de las mejores destilerías",
                imagen = "johnnie_walker_blue",
                precioVenta = 250.00,
                precioMayor = 200.00,
                precioCompra = 150.00,
                stock = 10,
                stockMinimo = 2,
                categoria = "Whisky",
                marca = "Johnnie Walker"
            ),
            ProductoData(
                nombre = "Johnnie Walker Black Label",
                descripcion = "Blended whisky escocés de 12 años, con carácter ahumado",
                imagen = "johnnie_walker_black",
                precioVenta = 65.00,
                precioMayor = 52.00,
                precioCompra = 39.00,
                stock = 25,
                stockMinimo = 8,
                categoria = "Whisky",
                marca = "Johnnie Walker"
            ),
            ProductoData(
                nombre = "Jack Daniel's Old No. 7",
                descripcion = "Whisky americano de Tennessee, suave con notas de caramelo",
                imagen = "jack_daniels_old_no7",
                precioVenta = 45.00,
                precioMayor = 36.00,
                precioCompra = 27.00,
                stock = 30,
                stockMinimo = 10,
                categoria = "Whisky",
                marca = "Jack Daniel's"
            ),
            ProductoData(
                nombre = "Jameson Irish Whiskey",
                descripcion = "Whisky irlandés triple destilado, suave y equilibrado",
                imagen = "jameson_irish",
                precioVenta = 40.00,
                precioMayor = 32.00,
                precioCompra = 24.00,
                stock = 25,
                stockMinimo = 8,
                categoria = "Whisky",
                marca = "Jameson"
            ),

            // TEQUILA
            ProductoData(
                nombre = "Clase Azul Reposado",
                descripcion = "Tequila reposado artesanal, añejado 8 meses en barricas de roble",
                imagen = "clase_azul_reposado",
                precioVenta = 210.00,
                precioMayor = 168.00,
                precioCompra = 126.00,
                stock = 8,
                stockMinimo = 2,
                categoria = "Tequila",
                marca = "Clase Azul"
            ),
            ProductoData(
                nombre = "Don Julio 1942",
                descripcion = "Tequila añejo premium, homenaje al año de fundación",
                imagen = "don_julio_1942",
                precioVenta = 150.00,
                precioMayor = 120.00,
                precioCompra = 90.00,
                stock = 15,
                stockMinimo = 4,
                categoria = "Tequila",
                marca = "Don Julio"
            ),
            ProductoData(
                nombre = "Don Julio Blanco",
                descripcion = "Tequila blanco cristalino, con notas de agave y cítricos",
                imagen = "don_julio_blanco",
                precioVenta = 45.00,
                precioMayor = 36.00,
                precioCompra = 27.00,
                stock = 30,
                stockMinimo = 10,
                categoria = "Tequila",
                marca = "Don Julio"
            ),
            ProductoData(
                nombre = "Patrón Silver",
                descripcion = "Tequila ultra premium blanco, suave y elegante",
                imagen = "patron_silver",
                precioVenta = 55.00,
                precioMayor = 44.00,
                precioCompra = 33.00,
                stock = 20,
                stockMinimo = 6,
                categoria = "Tequila",
                marca = "Patrón"
            ),

            // VINO
            ProductoData(
                nombre = "Casillero del Diablo Reserva Cabernet",
                descripcion = "Vino tinto chileno, intenso con notas de frutos rojos",
                imagen = "casillero_cabernet",
                precioVenta = 25.00,
                precioMayor = 20.00,
                precioCompra = 15.00,
                stock = 35,
                stockMinimo = 12,
                categoria = "Vino",
                marca = "Casillero del Diablo"
            ),
            ProductoData(
                nombre = "Concha y Toro Gran Reserva",
                descripcion = "Vino tinto chileno de alta calidad, cepa Carmenere",
                imagen = "concha_y_toro_gran_reserva",
                precioVenta = 30.00,
                precioMayor = 24.00,
                precioCompra = 18.00,
                stock = 25,
                stockMinimo = 8,
                categoria = "Vino",
                marca = "Concha y Toro"
            ),
            ProductoData(
                nombre = "Santa Rita 120 Merlot",
                descripcion = "Vino tinto chileno afrutado y equilibrado",
                imagen = "santa_rita_120_merlot",
                precioVenta = 18.00,
                precioMayor = 14.40,
                precioCompra = 10.80,
                stock = 40,
                stockMinimo = 15,
                categoria = "Vino",
                marca = "Santa Rita"
            ),

            // COGNAC
            ProductoData(
                nombre = "Hennessy X.O",
                descripcion = "Cognac extra old, complejo con notas de frutas y especias",
                imagen = "hennessy_xo",
                precioVenta = 450.00,
                precioMayor = 360.00,
                precioCompra = 270.00,
                stock = 5,
                stockMinimo = 1,
                categoria = "Cognac",
                marca = "Hennessy"
            ),
            ProductoData(
                nombre = "Hennessy V.S",
                descripcion = "Cognac very special, versátil y equilibrado",
                imagen = "hennessy_vs",
                precioVenta = 80.00,
                precioMayor = 64.00,
                precioCompra = 48.00,
                stock = 15,
                stockMinimo = 5,
                categoria = "Cognac",
                marca = "Hennessy"
            ),
            ProductoData(
                nombre = "Rémy Martin V.S.O.P",
                descripcion = "Cognac de alta calidad, añejado en barricas de roble",
                imagen = "remy_martin_vsop",
                precioVenta = 95.00,
                precioMayor = 76.00,
                precioCompra = 57.00,
                stock = 12,
                stockMinimo = 4,
                categoria = "Cognac",
                marca = "Rémy Martin"
            ),

            // VODKA
            ProductoData(
                nombre = "Grey Goose Vodka",
                descripcion = "Vodka premium francés, elaborado con trigo de alta calidad",
                imagen = "grey_goose_vodka",
                precioVenta = 55.00,
                precioMayor = 44.00,
                precioCompra = 33.00,
                stock = 20,
                stockMinimo = 6,
                categoria = "Vodka",
                marca = "Grey Goose"
            ),
            ProductoData(
                nombre = "Absolut Vodka",
                descripcion = "Vodka sueco, puro y versátil para cócteles",
                imagen = "absolut_vodka",
                precioVenta = 35.00,
                precioMayor = 28.00,
                precioCompra = 21.00,
                stock = 35,
                stockMinimo = 12,
                categoria = "Vodka",
                marca = "Absolut"
            ),
            ProductoData(
                nombre = "Belvedere Vodka",
                descripcion = "Vodka premium polaco, de trigo y centeno",
                imagen = "belvedere_vodka",
                precioVenta = 50.00,
                precioMayor = 40.00,
                precioCompra = 30.00,
                stock = 15,
                stockMinimo = 5,
                categoria = "Vodka",
                marca = "Belvedere"
            ),

            // CHAMPAGNE
            ProductoData(
                nombre = "Veuve Clicquot Brut",
                descripcion = "Champagne francés, equilibrado con notas de frutas",
                imagen = "veuve_clicquot_brut",
                precioVenta = 80.00,
                precioMayor = 64.00,
                precioCompra = 48.00,
                stock = 10,
                stockMinimo = 3,
                categoria = "Champagne",
                marca = "Veuve Clicquot"
            ),
            ProductoData(
                nombre = "Moët & Chandon Impérial",
                descripcion = "Champagne francés icónico, fresco y frutal",
                imagen = "moet_imperial",
                precioVenta = 70.00,
                precioMayor = 56.00,
                precioCompra = 42.00,
                stock = 12,
                stockMinimo = 4,
                categoria = "Champagne",
                marca = "Moët & Chandon"
            ),

            // RON
            ProductoData(
                nombre = "Bacardi Superior",
                descripcion = "Ron blanco ligero, perfecto para cócteles",
                imagen = "bacardi_superior",
                precioVenta = 30.00,
                precioMayor = 24.00,
                precioCompra = 18.00,
                stock = 30,
                stockMinimo = 10,
                categoria = "Ron",
                marca = "Bacardi"
            ),
            ProductoData(
                nombre = "Havana Club 7 Años",
                descripcion = "Ron cubano añejo, suave con notas de caramelo",
                imagen = "havana_club_7",
                precioVenta = 45.00,
                precioMayor = 36.00,
                precioCompra = 27.00,
                stock = 20,
                stockMinimo = 6,
                categoria = "Ron",
                marca = "Havana Club"
            ),
            ProductoData(
                nombre = "Zacapa 23 Solera",
                descripcion = "Ron guatemalteco premium, complejo y dulce",
                imagen = "zacapa_23",
                precioVenta = 85.00,
                precioMayor = 68.00,
                precioCompra = 51.00,
                stock = 10,
                stockMinimo = 3,
                categoria = "Ron",
                marca = "Zacapa"
            ),

            // GIN
            ProductoData(
                nombre = "Bombay Sapphire",
                descripcion = "Gin inglés con botánicos exóticos, suave y aromático",
                imagen = "bombay_sapphire",
                precioVenta = 40.00,
                precioMayor = 32.00,
                precioCompra = 24.00,
                stock = 20,
                stockMinimo = 6,
                categoria = "Gin",
                marca = "Bombay Sapphire"
            ),
            ProductoData(
                nombre = "Tanqueray London Dry",
                descripcion = "Gin inglés seco, con enebro y cítricos",
                imagen = "tanqueray_london_dry",
                precioVenta = 38.00,
                precioMayor = 30.40,
                precioCompra = 22.80,
                stock = 18,
                stockMinimo = 5,
                categoria = "Gin",
                marca = "Tanqueray"
            ),

            // LICORES
            ProductoData(
                nombre = "Baileys Irish Cream",
                descripcion = "Licor irlandés de crema con whisky, suave y dulce",
                imagen = "baileys_irish_cream",
                precioVenta = 35.00,
                precioMayor = 28.00,
                precioCompra = 21.00,
                stock = 15,
                stockMinimo = 5,
                categoria = "Licores",
                marca = "Jameson"
            ),
            ProductoData(
                nombre = "Kahlúa Coffee Liqueur",
                descripcion = "Licor de café mexicano, ideal para cócteles",
                imagen = "kahlua_coffee_liqueur",
                precioVenta = 30.00,
                precioMayor = 24.00,
                precioCompra = 18.00,
                stock = 12,
                stockMinimo = 4,
                categoria = "Licores",
                marca = "Don Julio"
            )
        )

        productos.forEach { producto ->
            val idCategoria = categoriasIds[producto.categoria] ?: 1
            val idMarca = marcasIds[producto.marca] ?: 1

            val values = ContentValues().apply {
                put("nombre_producto", producto.nombre)
                put("descripcion", producto.descripcion)
                put("imagen", producto.imagen)
                put("precio_venta", producto.precioVenta)
                put("precio_mayor", producto.precioMayor)
                put("precio_compra", producto.precioCompra)
                put("stock", producto.stock)
                put("stock_minimo", producto.stockMinimo)
                put("estado", ESTADO_ACTIVO)
                put("id_categoria", idCategoria)
                put("marcas_id_marca", idMarca)
            }
            db.insert("productos", null, values)
        }

        Log.d(TAG, "${productos.size} productos reales insertados correctamente")
    }

    // =============================================
    // DATA CLASS PARA PRODUCTOS
    // =============================================
    data class ProductoData(
        val nombre: String,
        val descripcion: String,
        val imagen: String,
        val precioVenta: Double,
        val precioMayor: Double,
        val precioCompra: Double,
        val stock: Int,
        val stockMinimo: Int,
        val categoria: String,
        val marca: String
    )

    // =============================================
    // ON UPGRADE
    // =============================================
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Log.d(TAG, "Actualizando base de datos de versión $oldVersion a $newVersion")

        // Eliminar en orden inverso a la creación
        val tablas = listOf(
            "movimientos_inventario",
            "proveedor_productos",
            "pagos_compra",
            "turnos_caja",
            "detalle_compra",
            "detalle_venta",
            "ventas",
            "compras",
            "reportes",
            "productos",
            "proveedores",
            "cuenta",
            "pagos",
            "marcas",
            "categorias",
            "roles",
            "persona"
        )

        tablas.forEach { tabla ->
            db.execSQL("DROP TABLE IF EXISTS $tabla")
        }

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

    // =============================================
    // MÉTODOS DE UTILIDAD
    // =============================================

    /*fun validarPin(pin: String): Boolean {
        var db: SQLiteDatabase? = null
        var cursor: android.database.Cursor? = null
        try {
            db = readableDatabase
            cursor = db.query(
                "cuenta",
                arrayOf("id_cuenta"),
                "clave = ? AND estado = ?",
                arrayOf(pin, ESTADO_ACTIVO),
                null, null, null
            )
            val esValido = cursor.count > 0
            Log.d(TAG, "Validando PIN: $pin, resultado: $esValido")
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
    }*/
    fun validarPin(pin: String): Boolean {
        val db = readableDatabase
        val cursor = db.query("cuenta", arrayOf("clave"), "estado = ?", arrayOf(ESTADO_ACTIVO), null, null, null)
        var valido = false
        cursor.use {
            while (it.moveToNext()) {
                val guardado = it.getString(it.getColumnIndexOrThrow("clave"))
                if (com.example.scarlet.util.PasswordUtils.verificar(pin, guardado)) { valido = true; break }
            }
        }
        db.close()
        return valido
    }
    fun obtenerUsuarioPorPin(pin: String): Map<String, String>? {
        var db: SQLiteDatabase? = null
        var cursor: android.database.Cursor? = null
        try {
            db = readableDatabase
            val query = """
                SELECT 
                    c.usuario,
                    p.nombres,
                    p.apellidos,
                    r.nombre_rol
                FROM cuenta c
                INNER JOIN persona p ON c.id_persona = p.id_persona
                INNER JOIN roles r ON c.id_rol = r.id_rol
                WHERE c.clave = ? AND c.estado = 'ACTIVO'
            """.trimIndent()

            cursor = db.rawQuery(query, arrayOf(pin))

            if (cursor.moveToFirst()) {
                val usuario = mapOf(
                    "usuario" to cursor.getString(cursor.getColumnIndexOrThrow("usuario")),
                    "nombres" to cursor.getString(cursor.getColumnIndexOrThrow("nombres")),
                    "apellidos" to cursor.getString(cursor.getColumnIndexOrThrow("apellidos")),
                    "nombre_rol" to cursor.getString(cursor.getColumnIndexOrThrow("nombre_rol"))
                )
                return usuario
            }
            return null
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener usuario: ${e.message}")
            return null
        } finally {
            cursor?.close()
            db?.close()
        }
    }

    fun obtenerTodosLosUsuarios(): List<Map<String, String>> {
        val usuarios = mutableListOf<Map<String, String>>()
        var db: SQLiteDatabase? = null
        var cursor: android.database.Cursor? = null
        try {
            db = readableDatabase
            val query = """
                SELECT 
                    c.id_cuenta,
                    c.usuario,
                    c.estado,
                    c.fecha_creacion,
                    p.nombres,
                    p.apellidos,
                    p.ci,
                    p.telefono,
                    r.nombre_rol
                FROM cuenta c
                INNER JOIN persona p ON c.id_persona = p.id_persona
                INNER JOIN roles r ON c.id_rol = r.id_rol
                ORDER BY c.id_cuenta
            """.trimIndent()

            cursor = db.rawQuery(query, null)

            while (cursor.moveToNext()) {
                val usuario = mapOf(
                    "id_cuenta" to cursor.getInt(cursor.getColumnIndexOrThrow("id_cuenta")).toString(),
                    "usuario" to cursor.getString(cursor.getColumnIndexOrThrow("usuario")),
                    "estado" to cursor.getString(cursor.getColumnIndexOrThrow("estado")),
                    "fecha_creacion" to cursor.getString(cursor.getColumnIndexOrThrow("fecha_creacion")),
                    "nombres" to cursor.getString(cursor.getColumnIndexOrThrow("nombres")),
                    "apellidos" to cursor.getString(cursor.getColumnIndexOrThrow("apellidos")),
                    "ci" to cursor.getString(cursor.getColumnIndexOrThrow("ci")),
                    "telefono" to cursor.getString(cursor.getColumnIndexOrThrow("telefono")),
                    "nombre_rol" to cursor.getString(cursor.getColumnIndexOrThrow("nombre_rol"))
                )
                usuarios.add(usuario)
            }
            return usuarios
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener usuarios: ${e.message}")
            return emptyList()
        } finally {
            cursor?.close()
            db?.close()
        }
    }
}