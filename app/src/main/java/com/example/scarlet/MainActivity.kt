package com.example.scarlet

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.scarlet.adapter.ProductosAdapter
import com.example.scarlet.data.model.Producto
import com.example.scarlet.data.repository.CuentaRepository
import com.example.scarlet.data.repository.ProductosRepository
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerViewProductos: RecyclerView
    private lateinit var adapter: ProductosAdapter
    private lateinit var productosRepository: ProductosRepository
    private lateinit var cuentaRepository: CuentaRepository
    private var listaProductos = mutableListOf<Producto>()

    private lateinit var bottomNavigation: BottomNavigationView

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

        // Cargar información del usuario en el header
        cargarInformacionUsuario()

        // Configurar RecyclerView de "Productos Recientes"
        configurarRecyclerView()

        // Cargar productos desde la base de datos
        cargarProductos()

        // Configurar listeners de header / FAB / categorías
        configurarListeners()

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

    private fun cargarInformacionUsuario() {
        val txtNombre = findViewById<TextView>(R.id.txtNombre)
        val txtRol = findViewById<TextView>(R.id.txtRol)

        try {
            val usuarioInfo = cuentaRepository.obtenerUsuarioActual()

            if (usuarioInfo != null) {
                txtNombre.text = "${usuarioInfo.nombres} ${usuarioInfo.apellidos}"
                txtRol.text = "●  ${usuarioInfo.nombreRol}"
            } else {
                txtNombre.text = "Admin Sistema"
                txtRol.text = "●  Administrador"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            txtNombre.text = "Admin Sistema"
            txtRol.text = "●  Administrador"
        }
    }

    private fun configurarRecyclerView() {
        recyclerViewProductos = findViewById(R.id.recyclerViewProductos)
        recyclerViewProductos.layoutManager = LinearLayoutManager(this)

        adapter = ProductosAdapter(listaProductos) { producto ->
            Toast.makeText(
                this,
                "Seleccionado: ${producto.nombreProducto}",
                Toast.LENGTH_SHORT
            ).show()
        }

        recyclerViewProductos.adapter = adapter
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
            Toast.makeText(this, "Agregar nuevo producto", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(this, "Perfil de usuario", Toast.LENGTH_SHORT).show()
        }
    }

    fun recargarProductos() {
        cargarProductos()
    }
}
