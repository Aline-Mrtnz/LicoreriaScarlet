package com.example.scarlet

import com.example.scarlet.util.NavegacionOrigen

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.scarlet.adapter.ProductosAdapter
import com.example.scarlet.cart.CartManager
import com.example.scarlet.data.model.Categorias
import com.example.scarlet.data.model.Marcas
import com.example.scarlet.data.model.Producto
import com.example.scarlet.data.repository.CategoriasRepository
import com.example.scarlet.data.repository.CuentaRepository
import com.example.scarlet.data.repository.MarcasRepository
import com.example.scarlet.data.repository.ProductosRepository
import com.example.scarlet.data.repository.VentasRepository
import com.example.scarlet.util.FechaUtils
import com.example.scarlet.util.Session
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.NumberFormat
import java.util.Locale
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.View
import android.widget.PopupWindow



class MainActivity : AppCompatActivity() {



    private lateinit var recyclerViewProductos: RecyclerView
    private lateinit var adapter: ProductosAdapter
    private lateinit var productosRepository: ProductosRepository
    private lateinit var cuentaRepository: CuentaRepository
    private lateinit var ventasRepository: VentasRepository
    private var listaProductos = mutableListOf<Producto>()

    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var tvCartBadge: TextView

    private val cartListener: () -> Unit = { actualizarBadgeCarrito() }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        //
        setContentView(R.layout.activity_main)

        val imgMenu = findViewById<ImageView>(R.id.imgMenu)
        val sideMenu = findViewById<LinearLayout>(R.id.sideMenu)
        val menuOverlay = findViewById<View>(R.id.viewMenuOverlay)
        val menuProveedores = findViewById<TextView>(R.id.menuProveedores)
        val menuMiCuenta = findViewById<TextView>(R.id.menuMiCuenta)

        fun abrirMenu() {
            menuOverlay.visibility = View.VISIBLE
            sideMenu.visibility = View.VISIBLE
            sideMenu.translationX = -sideMenu.width.toFloat()
            sideMenu.animate().translationX(0f).setDuration(250).start()
            resaltarItemMenuActual()
        }

        fun cerrarMenu() {
            sideMenu.animate()
                .translationX(-sideMenu.width.toFloat())
                .setDuration(200)
                .withEndAction {
                    sideMenu.visibility = View.GONE
                    menuOverlay.visibility = View.GONE
                }
                .start()
        }

        imgMenu.setOnClickListener {
            if (sideMenu.visibility == View.GONE) abrirMenu() else cerrarMenu()
        }

        // Cualquier toque fuera del menú (en el resto de la pantalla) lo cierra
        menuOverlay.setOnClickListener { cerrarMenu() }



        // para el mi cuenta
        menuMiCuenta.setOnClickListener {
            cerrarMenu()
            NavegacionOrigen.abrirModulo(this, MiCuenta::class.java)
        }
        findViewById<TextView>(R.id.menuCaja).setOnClickListener {
            cerrarMenu()
            NavegacionOrigen.abrirModulo(this, CajaActivity::class.java)
        }
        // para proveedores (antes no tenía listener: era inalcanzable)
        menuProveedores.setOnClickListener {
            cerrarMenu()
            NavegacionOrigen.abrirModulo(this, Proveedores::class.java)
        }
        // para categorías
        findViewById<TextView>(R.id.menuCategorias).setOnClickListener {
            cerrarMenu()
            NavegacionOrigen.abrirModulo(this, CategoriasActivity::class.java)
        }
        // para inventario (existía en el layout pero sin listener: era inalcanzable)
        findViewById<TextView>(R.id.menuInventario).setOnClickListener {
            cerrarMenu()
            NavegacionOrigen.abrirModulo(this, Inventario::class.java)
        }
        // para reabastecimiento / compras (idem: sin listener)
        findViewById<TextView>(R.id.menuReabastecimiento).setOnClickListener {
            cerrarMenu()
            NavegacionOrigen.abrirModulo(this, Reabastecimiento::class.java)
        }
        findViewById<TextView>(R.id.menuCajeros).setOnClickListener {
            cerrarMenu()
            NavegacionOrigen.abrirModulo(this, GestionCajeros::class.java)
        }
        // cerra sesion
        findViewById<TextView>(R.id.menuSalir).setOnClickListener {

            cerrarMenu()

            AlertDialog.Builder(this)
                .setTitle("Cerrar sesión")
                .setMessage("¿Estás seguro de que deseas cerrar sesión?")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Salir") { _, _ ->

                    Session.cerrar()
                    CartManager.limpiar()

                    val intent = Intent(this, Login::class.java)

                    intent.flags =
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TASK

                    startActivity(intent)
                    finish()
                }
                .show()
        }



        val header =
            findViewById<android.view.View>(R.id.headerLayout)

        bottomNavigation =
            findViewById(R.id.bottomNavigation)

        // Restringe accesos de gestión (Categorías, Proveedores, Inventario,
        // Reabastecimiento) a solo el rol Administrador. El Cajero también
        // ve Reportes (con sus propias estadísticas), además de Inicio,
        // Productos (catálogo/venta), Ventas y Mi cuenta.
        aplicarRestriccionesPorRol()

        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.main)
        ) { _, insets ->

            val systemBars =
                insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                )

            // ===============================
            // HEADER
            // ===============================

            val headerParams =
                header.layoutParams as ViewGroup.MarginLayoutParams

            headerParams.height =
                (82 * resources.displayMetrics.density).toInt() +
                        systemBars.top

            header.layoutParams = headerParams

            header.setPadding(
                header.paddingLeft,
                systemBars.top,
                header.paddingRight,
                header.paddingBottom
            )


            // ===============================
            // MENU INFERIOR
            // ===============================

            val bottomParams =
                bottomNavigation.layoutParams
                        as ViewGroup.MarginLayoutParams

            bottomParams.height =
                (80 * resources.displayMetrics.density).toInt() +
                        systemBars.bottom

            bottomNavigation.layoutParams = bottomParams

            bottomNavigation.setPadding(
                bottomNavigation.paddingLeft,
                bottomNavigation.paddingTop,
                bottomNavigation.paddingRight,
                systemBars.bottom
            )

            insets
        }

        // Inicializar repositorios
        productosRepository = ProductosRepository(this)
        cuentaRepository = CuentaRepository(this)
        ventasRepository = VentasRepository(this)
        tvCartBadge = findViewById(R.id.tvCartBadge)

        // Cargar información del usuario en el header
        cargarInformacionUsuario()

        // Configurar RecyclerView de "Productos Recientes"
        configurarRecyclerView()

        // Cargar productos desde la base de datos
        cargarProductos()

        // Cargar tarjetas de estadísticas (ventas de hoy, productos, stock bajo)
        cargarEstadisticas()

        // Configurar listeners de header / FAB / categorías
        configurarListeners()

        // Configurar notificaciones
        setupNotifications()

        // ============================================
        // BOTTOM NAVIGATION
        // ============================================

        bottomNavigation.selectedItemId = R.id.nav_home

        bottomNavigation.setOnItemSelectedListener { item ->

            when (item.itemId) {

                R.id.nav_home -> {
                    cargarProductos()
                    true
                }

                R.id.nav_productos -> {

                    startActivity(
                        Intent(
                            this,
                            Productos::class.java
                        )
                    )

                    true
                }

                R.id.nav_ventas -> {

                    startActivity(
                        Intent(
                            this,
                            Ventas::class.java
                        )
                    )

                    true
                }

                R.id.nav_reportes -> {

                    startActivity(
                        Intent(
                            this,
                            Reportes::class.java
                        )
                    )

                    true
                }

                else -> false
            }
        }
    }

    /*override fun onResume() {
        super.onResume()
        CartManager.agregarListener(cartListener)
        actualizarBadgeCarrito()
        cargarEstadisticas()
    }*/
    override fun onResume() {
        super.onResume()
        cargarEstadisticas()
        setupNotifications()   // <-- recalcula la campanita cada vez que vuelves a Home
    }

    override fun onPause() {
        super.onPause()
        CartManager.quitarListener(cartListener)
    }
    private fun resaltarItemMenuActual() {
        // Mapea cada item del menú con la Activity a la que navega.
        // null = no navega a otra Activity (ej. "Salir"), nunca se resalta.
        val items = listOf(
            findViewById<TextView>(R.id.menuMiCuenta) to MiCuenta::class.java,
            findViewById<TextView>(R.id.menuCaja) to CajaActivity::class.java,
            findViewById<TextView>(R.id.menuCategorias) to CategoriasActivity::class.java,
            findViewById<TextView>(R.id.menuProveedores) to Proveedores::class.java,
            findViewById<TextView>(R.id.menuInventario) to Inventario::class.java,
            findViewById<TextView>(R.id.menuReabastecimiento) to Reabastecimiento::class.java,
            findViewById<TextView>(R.id.menuCajeros) to GestionCajeros::class.java
        )

        items.forEach { (item, clase) ->
            val esActual = clase == this::class.java
            item.setBackgroundResource(
                if (esActual) R.drawable.bg_menu_item_selected else android.R.color.transparent
            )
            item.setTextColor(if (esActual) 0xFFFF3B16.toInt() else 0xFFCCCCCC.toInt())
        }
    }
    private fun actualizarBadgeCarrito() {
        val total = CartManager.totalItems()
        if (total > 0) {
            tvCartBadge.text = if (total > 99) "99+" else total.toString()
            tvCartBadge.visibility = android.view.View.VISIBLE
        } else {
            tvCartBadge.visibility = android.view.View.GONE
        }
    }

    private fun cargarInformacionUsuario() {
        val txtNombre = findViewById<TextView>(R.id.txtNombre)
        val txtRol = findViewById<TextView>(R.id.txtRol)

        try {
            if (Session.estaLogueado) {
                txtNombre.text = Session.nombreCompleto
                txtRol.text = "●  ${Session.rol}"
            } else {
                val actual = cuentaRepository.obtenerUsuarioActual()
                if (actual != null) {
                    txtNombre.text = "${actual.nombres} ${actual.apellidos}"
                    txtRol.text = "●  ${actual.nombreRol}"
                } else {
                    txtNombre.text = "Admin Sistema"
                    txtRol.text = "●  Administrador"
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            txtNombre.text = "Admin Sistema"
            txtRol.text = "●  Administrador"
        }
    }

    private fun cargarEstadisticas() {
        try {
            val (desde, hasta) = FechaUtils.rangoParaFiltro("Día")
            val totalHoy = ventasRepository.totalEntreFechas(desde, hasta)
            findViewById<TextView>(R.id.txtVentasHoy).text =
                "Bs " + String.format(Locale("es", "BO"), "%,.2f", totalHoy)

            val stats = productosRepository.obtenerEstadisticasProductos()
            findViewById<TextView>(R.id.txtProductosActivos).text = (stats["total"] ?: 0).toString()
            findViewById<TextView>(R.id.txtStockBajo).text = "${stats["stock_bajo"] ?: 0} Alertas"
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun configurarRecyclerView() {
        recyclerViewProductos = findViewById(R.id.recyclerViewProductos)
        recyclerViewProductos.layoutManager = LinearLayoutManager(this)

        adapter = ProductosAdapter(listaProductos) { producto ->
            agregarAlCarrito(producto)
        }

        recyclerViewProductos.adapter = adapter
    }

    private fun agregarAlCarrito(producto: Producto) {
        when (CartManager.agregarProducto(producto)) {
            CartManager.ResultadoAgregar.AGREGADO -> {
                Toast.makeText(this, "${producto.nombreProducto} añadido al carrito", Toast.LENGTH_SHORT).show()
            }
            CartManager.ResultadoAgregar.SIN_STOCK -> {
                Toast.makeText(this, "Sin stock disponible", Toast.LENGTH_SHORT).show()
            }
            CartManager.ResultadoAgregar.STOCK_MAXIMO -> {
                Toast.makeText(this, "Alcanzaste el stock máximo disponible", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun cargarProductos() {
        try {
            val productos = productosRepository.obtenerTodosLosProductos()
            listaProductos.clear()
            listaProductos.addAll(productos)
            adapter.actualizarProductos(listaProductos)

            if (productos.isEmpty()) {
                Toast.makeText(this, "No hay productos disponibles", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error al cargar productos: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun configurarListeners() {
        val fabAdd = findViewById<FloatingActionButton>(R.id.fabAdd)

        fabAdd.setOnClickListener {
            startActivity(Intent(this, QR::class.java))
        }

        val verCategorias = findViewById<TextView>(R.id.idVerCategorias)
        verCategorias.setOnClickListener {
            startActivity(Intent(this, Productos::class.java))
        }

        /*val imgCarrito = findViewById<ImageView>(R.id.imgCarrito)
        imgCarrito.setOnClickListener {
            if (com.example.scarlet.cart.CartManager.estaVacio()) {
                Toast.makeText(this, "Tu carrito está vacío. Agrega productos primero.", Toast.LENGTH_SHORT).show()
            } else {
                startActivity(Intent(this, Shopping::class.java))
            }
        }*/
        val imgCarrito = findViewById<ImageView>(R.id.imgCarrito)
        imgCarrito.setOnClickListener {
            com.example.scarlet.util.CarritoUtils.manejarClick(this, imgCarrito)
        }

        val imgPerfil = findViewById<ImageView>(R.id.imgPerfil)
        imgPerfil.setOnClickListener {
            mostrarDialogoPerfil()
        }

        findViewById<android.view.View>(R.id.cardCatWhisky).setOnClickListener { abrirCategoria("Whisky") }
        findViewById<android.view.View>(R.id.cardCatVino).setOnClickListener { abrirCategoria("Vino") }
        findViewById<android.view.View>(R.id.cardCatCerveza).setOnClickListener { abrirCategoria("Cerveza") }
        findViewById<android.view.View>(R.id.cardCatTequila).setOnClickListener { abrirCategoria("Tequila") }
    }

    private fun abrirCategoria(nombreCategoria: String) {
        val intent = Intent(this, Productos::class.java)
        intent.putExtra(Productos.EXTRA_CATEGORIA, nombreCategoria)
        startActivity(intent)
    }

    private fun mostrarDialogoPerfil() {
        val nombre = if (Session.estaLogueado) Session.nombreCompleto else "Admin Sistema"
        val rol = if (Session.estaLogueado) Session.rol else "Administrador"

        AlertDialog.Builder(this)
            .setTitle(nombre)
            .setMessage("Rol: $rol")
            .setPositiveButton("Cerrar sesión") { _, _ ->
                Session.cerrar()
                CartManager.limpiar()
                val intent = Intent(this, Login::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // ============================================
    // NOTIFICACIONES
    // ============================================

    private fun setupNotifications() {
        com.example.scarlet.util.AlertasUtils.configurar(this)
    }

    fun recargarProductos() {
        cargarProductos()
    }

    /**
     * Oculta del menú lateral las secciones de gestión exclusivas de
     * Administrador (Categorías, Proveedores, Inventario, Reabastecimiento).
     * El Cajero conserva Inicio, Productos, Ventas, Mi cuenta — y también
     * Reportes.
     *
     * ANTES esta función también ocultaba la pestaña "Reportes" del menú
     * inferior para cualquier cuenta que no fuera Administrador, así que un
     * Cajero no tenía ninguna forma de llegar a esa pantalla. Reportes.kt ya
     * está preparado para mostrarle a cada cajero SOLO sus propias
     * estadísticas/ventas (y reservar la vista general — todas las cuentas —
     * únicamente para el Administrador), así que ya no hace falta esconderle
     * la pestaña.
     */
    private fun aplicarRestriccionesPorRol() {
        if (Session.esAdmin) return

        findViewById<TextView>(R.id.menuCategorias).visibility = android.view.View.GONE
        findViewById<TextView>(R.id.menuCajeros).visibility = android.view.View.GONE
        findViewById<TextView>(R.id.menuProveedores).visibility = android.view.View.GONE
        findViewById<TextView>(R.id.menuInventario).visibility = android.view.View.GONE
        findViewById<TextView>(R.id.menuReabastecimiento).visibility = android.view.View.GONE
    }
}