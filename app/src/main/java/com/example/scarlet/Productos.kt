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
import com.example.scarlet.data.repository.CategoriasRepository
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
    private lateinit var categoriasRepository: CategoriasRepository
    private lateinit var edtBuscar: EditText
    private lateinit var tvCartBadge: TextView
    private val decimalFormat = DecimalFormat("Bs #,##0.00")

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
        // Restringe accesos de gestión a solo el rol Administrador.
        if (!Session.esAdmin) {
            findViewById<TextView>(R.id.menuCategorias).visibility = View.GONE
            menuProveedores.visibility = View.GONE
            findViewById<TextView>(R.id.menuInventario).visibility = View.GONE
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
        categoriasRepository = CategoriasRepository(this)
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
        val contenedorChips = findViewById<LinearLayout>(R.id.catChipsContainer)
        val chipTodos = findViewById<TextView>(R.id.catTodos)

        // Quitar cualquier chip generado dinámicamente en una carga anterior
        // (por ejemplo al volver de Gestión de Categorías con onResume),
        // dejando solo el chip fijo "Todos".
        while (contenedorChips.childCount > 1) {
            contenedorChips.removeViewAt(contenedorChips.childCount - 1)
        }

        val chipsMutable = mutableMapOf<Int, String>(R.id.catTodos to "Todos")

        chipTodos.setOnClickListener {
            seleccionarChip(chipTodos)
            categoriaActual = "Todos"
            edtBuscar.text?.clear()
            cargarProductos()
        }

        // Solo se muestran aquí las categorías ACTIVAS que ya tienen al menos
        // un producto registrado. Una categoría recién creada sin productos
        // no aparece en este listado hasta que se le asigne uno.
        val categoriasVisibles = categoriasRepository.listarVisiblesEnCatalogo()

        categoriasVisibles.forEach { categoria ->
            val nuevoId = View.generateViewId()
            val chip = TextView(this).apply {
                id = nuevoId
                text = categoria.nombre_categoria
                textSize = 12f
                setTextColor(0xFF777777.toInt())
                setBackgroundResource(R.drawable.bg_category_unselected)
                gravity = android.view.Gravity.CENTER
                setPadding(dpAProductos(16), 0, dpAProductos(16), 0)
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    dpAProductos(32)
                )
                params.marginEnd = dpAProductos(8)
                layoutParams = params
            }
            contenedorChips.addView(chip)
            chipsMutable[nuevoId] = categoria.nombre_categoria

            chip.setOnClickListener {
                seleccionarChip(chip)
                categoriaActual = categoria.nombre_categoria
                edtBuscar.text?.clear()
                cargarProductos()
            }
        }

        chips = chipsMutable

        // Si la categoría que estaba seleccionada ya no está disponible
        // (por ejemplo se desactivó desde Gestión de Categorías), volvemos a "Todos".
        if (categoriaActual !in chipsMutable.values) {
            categoriaActual = "Todos"
        }

        chipSeleccionado = contenedorChips.children()
            .firstOrNull { (it as? TextView)?.text == categoriaActual } as? TextView
            ?: chipTodos
        seleccionarChip(chipSeleccionado!!)
    }

    private fun dpAProductos(valor: Int): Int {
        return (valor * resources.displayMetrics.density).toInt()
    }

    private fun LinearLayout.children(): List<View> {
        return (0 until childCount).map { getChildAt(it) }
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
            if (com.example.scarlet.cart.CartManager.estaVacio()) {
                Toast.makeText(this, "Tu carrito está vacío. Agrega productos primero.", Toast.LENGTH_SHORT).show()
            } else {
                startActivity(Intent(this, Shopping::class.java))
            }
        }
        findViewById<ImageView>(R.id.btnAddProducto).apply {
            // Cajero no puede dar de alta productos: se oculta el botón y,
            // como refuerzo, AgregarProducto también valida el rol al abrir.
            visibility = if (Session.esAdmin) View.VISIBLE else View.GONE
            setOnClickListener {
                agregarProductoLauncher.launch(Intent(this@Productos, AgregarProducto::class.java))
            }
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