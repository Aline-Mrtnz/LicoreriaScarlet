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

        setContentView(R.layout.activity_main)

        val header =
            findViewById<android.view.View>(R.id.headerLayout)

        bottomNavigation =
            findViewById(R.id.bottomNavigation)

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
            val formato = NumberFormat.getCurrencyInstance(Locale.US)
            findViewById<TextView>(R.id.txtVentasHoy).text = formato.format(totalHoy)

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
            //mostrarDialogoNuevoProducto()
        }

        val verCategorias = findViewById<TextView>(R.id.idVerCategorias)
        verCategorias.setOnClickListener {
            startActivity(Intent(this, Productos::class.java))
        }

        val imgCarrito = findViewById<ImageView>(R.id.imgCarrito)
        imgCarrito.setOnClickListener {
            startActivity(Intent(this, Shopping::class.java))
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
        }

    fun recargarProductos() {
        cargarProductos()
    }
}
