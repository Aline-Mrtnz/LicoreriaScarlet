package com.example.scarlet

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
        val menuProveedores = findViewById<TextView>(R.id.menuProveedores)
        val menuMiCuenta = findViewById<TextView>(R.id.menuMiCuenta)
        val fabAdd = findViewById<FloatingActionButton>(R.id.fabAdd)

        fabAdd.setOnClickListener {
            startActivity(
                Intent(this, QR::class.java)
            )
        }

        imgMenu.setOnClickListener {

            if (sideMenu.visibility == View.GONE) {

                sideMenu.visibility = View.VISIBLE

                sideMenu.translationX = -sideMenu.width.toFloat()

                sideMenu.animate()
                    .translationX(0f)
                    .setDuration(250)
                    .start()

            } else {

                sideMenu.animate()
                    .translationX(-sideMenu.width.toFloat())
                    .setDuration(250)
                    .withEndAction {
                        sideMenu.visibility = View.GONE
                    }
                    .start()
            }
        }



        // para el mi cuenta
        menuMiCuenta.setOnClickListener {
            val intent = Intent(this, MiCuenta::class.java)
            startActivity(intent)
        }
        findViewById<TextView>(R.id.menuCaja).setOnClickListener {
            startActivity(Intent(this, CajaActivity::class.java))
        }
        // para proveedores (antes no tenía listener: era inalcanzable)
        menuProveedores.setOnClickListener {
            startActivity(Intent(this, Proveedores::class.java))
        }
        // para categorías
        findViewById<TextView>(R.id.menuCategorias).setOnClickListener {
            startActivity(Intent(this, CategoriasActivity::class.java))
        }
        // para inventario (existía en el layout pero sin listener: era inalcanzable)
        findViewById<TextView>(R.id.menuInventario).setOnClickListener {
            startActivity(Intent(this, Inventario::class.java))
        }
        // para reabastecimiento / compras (idem: sin listener)
        findViewById<TextView>(R.id.menuReabastecimiento).setOnClickListener {
            startActivity(Intent(this, Reabastecimiento::class.java))
        }
        findViewById<TextView>(R.id.menuCajeros).setOnClickListener {
            startActivity(Intent(this, GestionCajeros::class.java))
        }
        // cerra sesion
        findViewById<TextView>(R.id.menuSalir).setOnClickListener {

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
        // Reabastecimiento, Reportes) a solo el rol Administrador. El Cajero
        // solo debe ver Inicio, Productos (catálogo/venta), Ventas y Mi cuenta.
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

    override fun onResume() {
        super.onResume()
        CartManager.agregarListener(cartListener)
        actualizarBadgeCarrito()
        cargarEstadisticas()
    }

    override fun onPause() {
        super.onPause()
        CartManager.quitarListener(cartListener)
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

        val imgCarrito = findViewById<ImageView>(R.id.imgCarrito)
        imgCarrito.setOnClickListener {
            if (com.example.scarlet.cart.CartManager.estaVacio()) {
                Toast.makeText(this, "Tu carrito está vacío. Agrega productos primero.", Toast.LENGTH_SHORT).show()
            } else {
                startActivity(Intent(this, Shopping::class.java))
            }
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

    /*private fun setupNotifications() {

        val notificationIcon =
            findViewById<ImageView>(R.id.imgNorificacion)

        val badge =
            findViewById<TextView>(R.id.txtNotificationBadge)

        val notificationCount = 3

        if (notificationCount > 0) {

            badge.text =
                if (notificationCount > 99) {
                    "99+"
                } else {
                    notificationCount.toString()
                }

            badge.visibility =
                android.view.View.VISIBLE

        } else {

            badge.visibility =
                android.view.View.GONE
        }

        notificationIcon.setOnClickListener {

            Toast.makeText(
                this,
                "Tienes $notificationCount nuevas notificaciones",
                Toast.LENGTH_SHORT
            ).show()

            badge.visibility =
                android.view.View.GONE
        }
    }*/
    private fun setupNotifications() {
        com.example.scarlet.util.AlertasUtils.configurar(this)
    }

    fun recargarProductos() {
        cargarProductos()
    }

    /**
     * Oculta del menú lateral y de la barra inferior las secciones de
     * gestión (Categorías, Proveedores, Inventario, Reabastecimiento,
     * Reportes) cuando la cuenta que inició sesión no es Administrador.
     * El Cajero conserva Inicio, Productos, Ventas y Mi cuenta.
     */
    private fun aplicarRestriccionesPorRol() {
        if (Session.esAdmin) return

        findViewById<TextView>(R.id.menuCategorias).visibility = android.view.View.GONE
        findViewById<TextView>(R.id.menuCajeros).visibility = android.view.View.GONE
        findViewById<TextView>(R.id.menuProveedores).visibility = android.view.View.GONE
        findViewById<TextView>(R.id.menuInventario).visibility = android.view.View.GONE
        findViewById<TextView>(R.id.menuReabastecimiento).visibility = android.view.View.GONE

        bottomNavigation.menu.findItem(R.id.nav_reportes)?.isVisible = false
    }
}