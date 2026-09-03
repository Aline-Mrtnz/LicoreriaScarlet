package com.example.scarlet

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.scarlet.adapter.ProductosGridAdapter
import com.example.scarlet.cart.CartManager
import com.example.scarlet.data.model.Producto
import com.example.scarlet.data.repository.ProductosRepository
import java.text.DecimalFormat
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
//
import android.view.View
import android.widget.LinearLayout
import com.example.scarlet.util.Session




class Productos : AppCompatActivity() {

    companion object {
        const val EXTRA_CATEGORIA = "extra_categoria"
    }

    private lateinit var recyclerViewProductos: RecyclerView
    private lateinit var adapter: ProductosGridAdapter
    private lateinit var productosRepository: ProductosRepository
    private lateinit var edtBuscar: EditText
    private lateinit var tvCartBadge: TextView
    private val decimalFormat = DecimalFormat("$#,##0.00")

    private var categoriaActual: String = "Todos"

    // Chips de categoría -> nombre de categoría que se envía al repositorio
    private lateinit var chips: Map<Int, String>
    private var chipSeleccionado: TextView? = null

    private val cartListener: () -> Unit = { actualizarBadgeCarrito() }

    private val agregarProductoLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { resultado ->
        if (resultado.resultCode == RESULT_OK) {
            cargarProductos()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_products)
        //
        val imgMenu = findViewById<ImageView>(R.id.imgMenu)
        val sideMenu = findViewById<LinearLayout>(R.id.sideMenu)
        val menuProveedores = findViewById<TextView>(R.id.menuProveedores)
        val menuMiCuenta = findViewById<TextView>(R.id.menuMiCuenta)

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
        //


        window.statusBarColor = android.graphics.Color.BLACK

        window.decorView.systemUiVisibility = 0

        productosRepository = ProductosRepository(this)
        val bottomNavigation =
            findViewById<BottomNavigationView>(R.id.bottomNavigation)

        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.main)
        ) { _, insets ->

            val systemBars =
                insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                )

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
        // ============================================
        // NAVEGACIÓN INFERIOR
        // ============================================

        bottomNavigation.selectedItemId = R.id.nav_productos

        bottomNavigation.setOnItemSelectedListener { item ->

            when (item.itemId) {

                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                    true
                }

                R.id.nav_productos -> {
                    // Ya estamos en Productos
                    true
                }

                R.id.nav_ventas -> {
                    startActivity(Intent(this, Ventas::class.java))
                    finish()
                    true
                }

                R.id.nav_reportes -> {
                    startActivity(Intent(this, Reportes::class.java))
                    finish()
                    true
                }

                else -> false
            }
        }


        tvCartBadge = findViewById(R.id.tvCartBadge)

        configurarRecyclerView()
        configurarCategorias()
        configurarBusqueda()
        configurarListeners()
        setupNotifications()

        // Si venimos desde una categoría de la pantalla de Inicio, la preseleccionamos
        val categoriaSolicitada = intent.getStringExtra(EXTRA_CATEGORIA)
        if (!categoriaSolicitada.isNullOrBlank()) {
            categoriaActual = categoriaSolicitada
            val chipId = chips.entries.find { it.value == categoriaSolicitada }?.key
            if (chipId != null) {
                seleccionarChip(findViewById(chipId))
            }
        }

        cargarProductos()
    }

    override fun onResume() {
        super.onResume()
        CartManager.agregarListener(cartListener)
        actualizarBadgeCarrito()
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

    private fun configurarRecyclerView() {
        recyclerViewProductos = findViewById(R.id.recyclerViewProductos)
        recyclerViewProductos.layoutManager = GridLayoutManager(this, 2)

        adapter = ProductosGridAdapter(
            emptyList(),
            onAgregarClick = { producto -> agregarAlCarrito(producto) },
            onDescripcionClick = { producto -> mostrarDialogoDescripcion(producto) }
        )

        recyclerViewProductos.adapter = adapter
    }

    private fun mostrarDialogoDescripcion(producto: Producto) {
        val vista = LayoutInflater.from(this).inflate(R.layout.dialog_producto_detalle, null)

        val dialog = AlertDialog.Builder(this)
            .setView(vista)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        vista.findViewById<TextView>(R.id.tvDialogNombre).text = producto.nombreProducto
        vista.findViewById<TextView>(R.id.tvDialogDescripcion).text =
            producto.descripcion?.takeIf { it.isNotBlank() } ?: "Este producto no tiene una descripción registrada."
        vista.findViewById<TextView>(R.id.tvDialogPrecio).text = decimalFormat.format(producto.precioVenta)

        vista.findViewById<ImageView>(R.id.btnCerrarDialog).setOnClickListener {
            dialog.dismiss()
        }
        vista.findViewById<TextView>(R.id.btnDialogAgregarCarrito).setOnClickListener {
            agregarAlCarrito(producto)
            dialog.dismiss()
        }

        dialog.show()
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

    private fun configurarCategorias() {
        chips = mapOf(
            R.id.catTodos to "Todos",
            R.id.catWhisky to "Whisky",
            R.id.catTequila to "Tequila",
            R.id.catCognac to "Cognac",
            R.id.catVodka to "Vodka",
            R.id.catChampagne to "Champagne"
        )

        chipSeleccionado = findViewById(R.id.catTodos)

        chips.forEach { (id, nombreCategoria) ->
            val chip = findViewById<TextView>(id)
            chip.setOnClickListener {
                seleccionarChip(chip)
                categoriaActual = nombreCategoria
                edtBuscar.text?.clear()
                cargarProductos()
            }
        }
    }

    private fun seleccionarChip(nuevoChip: TextView) {
        chipSeleccionado?.apply {
            setBackgroundResource(R.drawable.bg_category_unselected)
            setTextColor(0xFF777777.toInt())
        }
        nuevoChip.apply {
            setBackgroundResource(R.drawable.bg_category_selected)
            setTextColor(0xFFFFFFFF.toInt())
        }
        chipSeleccionado = nuevoChip
    }

    private fun configurarBusqueda() {
        edtBuscar = findViewById(R.id.edtBuscar)
        edtBuscar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString().orEmpty().trim()
                if (query.isEmpty()) {
                    cargarProductos()
                } else {
                    try {
                        val resultados = productosRepository.buscarProductos(query)
                        adapter.actualizarProductos(resultados)
                    } catch (e: Exception) {
                        Toast.makeText(this@Productos, "Error al buscar: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun cargarProductos() {
        try {
            val productos: List<Producto> = if (categoriaActual == "Todos") {
                productosRepository.obtenerTodosLosProductos()
            } else {
                productosRepository.obtenerProductosPorCategoria(categoriaActual)
            }

            adapter.actualizarProductos(productos)

            if (productos.isEmpty()) {
                Toast.makeText(this, "No hay productos disponibles en esta categoría", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error al cargar productos: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun configurarListeners() {
        findViewById<ImageView>(R.id.imgCarrito).setOnClickListener {
            startActivity(Intent(this, Shopping::class.java))
        }
        findViewById<ImageView>(R.id.btnAddProducto).setOnClickListener {
            agregarProductoLauncher.launch(Intent(this, AgregarProducto::class.java))
        }
    }

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

            badge.visibility = android.view.View.VISIBLE

        } else {

            badge.visibility = android.view.View.GONE
        }

        notificationIcon.setOnClickListener {

            Toast.makeText(
                this,
                "Tienes $notificationCount nuevas notificaciones",
                Toast.LENGTH_SHORT
            ).show()

            badge.visibility = android.view.View.GONE
        }
    }
}
