package com.example.scarlet

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.scarlet.adapter.TopProductosAdapter
import com.example.scarlet.data.model.TopProducto
import com.google.android.material.bottomnavigation.BottomNavigationView

class Reportes : AppCompatActivity() {

    private lateinit var recyclerViewTopProductos: RecyclerView
    private lateinit var topProductosAdapter: TopProductosAdapter

    // Top productos de ejemplo por filtro. En una integración real esto
    // vendría de ReportesRepository / VentasRepository según el rango de fechas.
    private val topProductosPorFiltro = mapOf(
        "Día" to listOf(
            TopProducto("Macallan 18 Years", "Whisky Escocés", 349.00, 3, R.drawable.macallan18),
            TopProducto("Clase Azul Reposado", "Tequila Artesanal", 210.00, 2, R.drawable.clase_azul)
        ),
        "Semana" to listOf(
            TopProducto("Macallan 18 Years", "Whisky Escocés", 349.00, 24, R.drawable.macallan18),
            TopProducto("Clase Azul Reposado", "Tequila Artesanal", 210.00, 18, R.drawable.clase_azul)
        ),
        "Mes" to listOf(
            TopProducto("Macallan 18 Years", "Whisky Escocés", 349.00, 96, R.drawable.macallan18),
            TopProducto("Clase Azul Reposado", "Tequila Artesanal", 210.00, 74, R.drawable.clase_azul),
            TopProducto("Hennessy X.O", "Cognac", 280.00, 51, R.drawable.hennessyxo)
        ),
        "Año" to listOf(
            TopProducto("Macallan 18 Years", "Whisky Escocés", 349.00, 1120, R.drawable.macallan18),
            TopProducto("Clase Azul Reposado", "Tequila Artesanal", 210.00, 890, R.drawable.clase_azul),
            TopProducto("Hennessy X.O", "Cognac", 280.00, 640, R.drawable.hennessyxo)
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_reports)

        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.main)
        ) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )
            insets
        }

        configurarRecyclerView()
        setupFilters()
        setupBottomNavigation()
    }

    private fun configurarRecyclerView() {
        recyclerViewTopProductos = findViewById(R.id.recyclerViewTopProductos)
        recyclerViewTopProductos.layoutManager = LinearLayoutManager(this)
        recyclerViewTopProductos.isNestedScrollingEnabled = false

        topProductosAdapter = TopProductosAdapter(topProductosPorFiltro.getValue("Día"))
        recyclerViewTopProductos.adapter = topProductosAdapter
    }

    private fun setupFilters() {
        val filters = listOf(
            R.id.filterDay to "Día",
            R.id.filterWeek to "Semana",
            R.id.filterMonth to "Mes",
            R.id.filterYear to "Año"
        )

        var selectedView: TextView? = findViewById(R.id.filterDay)

        filters.forEach { (id, name) ->
            val view = findViewById<TextView>(id)
            view.setOnClickListener {
                // Resetear estilo del anterior
                selectedView?.apply {
                    setBackgroundResource(R.drawable.bg_filter_unselected)
                    setTextColor(resources.getColor(android.R.color.darker_gray))
                }

                // Establecer nuevo seleccionado
                view.apply {
                    setBackgroundResource(R.drawable.bg_filter_selected)
                    setTextColor(resources.getColor(android.R.color.white))
                }
                selectedView = view

                updateDataForFilter(name)
            }
        }
    }

    private fun updateDataForFilter(filter: String) {
        when (filter) {
            "Día" -> {
                findViewById<TextView>(R.id.txtTotalVentas).text = "$3,450"
                findViewById<TextView>(R.id.txtGanancia).text = "$1,120"
            }
            "Semana" -> {
                findViewById<TextView>(R.id.txtTotalVentas).text = "$24,850"
                findViewById<TextView>(R.id.txtGanancia).text = "$8,210"
            }
            "Mes" -> {
                findViewById<TextView>(R.id.txtTotalVentas).text = "$98,400"
                findViewById<TextView>(R.id.txtGanancia).text = "$32,500"
            }
            "Año" -> {
                findViewById<TextView>(R.id.txtTotalVentas).text = "$1,245,000"
                findViewById<TextView>(R.id.txtGanancia).text = "$412,000"
            }
        }

        topProductosPorFiltro[filter]?.let { topProductosAdapter.actualizar(it) }
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_reportes

        bottomNav.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.nav_productos -> {
                    val intent = Intent(this, Productos::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.nav_ventas -> {
                    val intent = Intent(this, Ventas::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.nav_reportes -> {
                    val intent = Intent(this, Reportes::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                /*R.id.nav_profile -> {
                    // Intent a Profile cuando exista
                    true
                }*/
                else -> false
            }
        }
    }
}
