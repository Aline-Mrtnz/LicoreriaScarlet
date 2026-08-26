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
import com.example.scarlet.data.model.Producto
import com.example.scarlet.data.repository.ProductosRepository

class Productos : AppCompatActivity() {

    private lateinit var recyclerViewProductos: RecyclerView
    private lateinit var adapter: ProductosAdapter
    private lateinit var productosRepository: ProductosRepository
    private lateinit var edtBuscar: EditText

    private var categoriaActual: String = "Todos"

    // Chips de categoría -> nombre de categoría que se envía al repositorio
    private lateinit var chips: Map<Int, String>
    private var chipSeleccionado: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_products)

        productosRepository = ProductosRepository(this)

        configurarRecyclerView()
        configurarCategorias()
        configurarBusqueda()
        configurarListeners()

        cargarProductos()
    }

    private fun configurarRecyclerView() {
        recyclerViewProductos = findViewById(R.id.recyclerViewProductos)
        recyclerViewProductos.layoutManager = LinearLayoutManager(this)

        adapter = ProductosAdapter(emptyList()) { producto ->
            Toast.makeText(this, "Seleccionado: ${producto.nombreProducto}", Toast.LENGTH_SHORT).show()
        }

        recyclerViewProductos.adapter = adapter
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
                Toast.makeText(this, "No hay productos disponibles", Toast.LENGTH_SHORT).show()
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
