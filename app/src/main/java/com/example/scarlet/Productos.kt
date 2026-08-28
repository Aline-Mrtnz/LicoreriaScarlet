package com.example.scarlet

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.scarlet.adapter.ProductosAdapter
import com.example.scarlet.cart.CartManager
import com.example.scarlet.data.model.Producto
import com.example.scarlet.data.repository.ProductosRepository

class Productos : AppCompatActivity() {

    companion object {
        const val EXTRA_CATEGORIA = "extra_categoria"
    }

    private lateinit var recyclerViewProductos: RecyclerView
    private lateinit var adapter: ProductosAdapter
    private lateinit var productosRepository: ProductosRepository
    private lateinit var edtBuscar: EditText
    private lateinit var tvCartBadge: TextView

    private var categoriaActual: String = "Todos"

    // Chips de categoría -> nombre de categoría que se envía al repositorio
    private lateinit var chips: Map<Int, String>
    private var chipSeleccionado: TextView? = null

    private val cartListener: () -> Unit = { actualizarBadgeCarrito() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_products)

        productosRepository = ProductosRepository(this)
        tvCartBadge = findViewById(R.id.tvCartBadge)

        configurarRecyclerView()
        configurarCategorias()
        configurarBusqueda()
        configurarListeners()

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
        recyclerViewProductos.layoutManager = LinearLayoutManager(this)

        adapter = ProductosAdapter(emptyList()) { producto ->
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
    }
}
