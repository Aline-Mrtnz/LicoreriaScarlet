package com.example.scarlet

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.scarlet.adapter.TopProductosAdapter
import com.example.scarlet.data.repository.ReportesRepository
import com.example.scarlet.data.repository.VentasRepository
import com.example.scarlet.util.FechaUtils
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.text.NumberFormat
import java.util.Locale

class Reportes : AppCompatActivity() {

    private lateinit var recyclerViewTopProductos: RecyclerView
    private lateinit var topProductosAdapter: TopProductosAdapter

    private lateinit var reportesRepository: ReportesRepository
    private lateinit var ventasRepository: VentasRepository

    private val formatoMoneda = NumberFormat.getCurrencyInstance(Locale.US)

    // Ids de las 7 barras/etiquetas del gráfico "Rendimiento" en el mismo orden
    private val barIds = listOf(R.id.barDia1, R.id.barDia2, R.id.barDia3, R.id.barDia4, R.id.barDia5, R.id.barDia6, R.id.barDia7)
    private val lblIds = listOf(R.id.lblDia1, R.id.lblDia2, R.id.lblDia3, R.id.lblDia4, R.id.lblDia5, R.id.lblDia6, R.id.lblDia7)

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

        reportesRepository = ReportesRepository(this)
        ventasRepository = VentasRepository(this)

        configurarRecyclerView()
        setupFilters()
        setupBottomNavigation()

        updateDataForFilter("Día")
        cargarGraficoRendimiento()
    }

    override fun onResume() {
        super.onResume()
        updateDataForFilter(filtroActual)
        cargarGraficoRendimiento()
    }

    private var filtroActual = "Día"

    private fun configurarRecyclerView() {
        recyclerViewTopProductos = findViewById(R.id.recyclerViewTopProductos)
        recyclerViewTopProductos.layoutManager = LinearLayoutManager(this)
        recyclerViewTopProductos.isNestedScrollingEnabled = false

        topProductosAdapter = TopProductosAdapter(emptyList())
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
        filtroActual = filter
        try {
            val (desde, hasta) = FechaUtils.rangoParaFiltro(filter)

            val totalVentas = ventasRepository.totalEntreFechas(desde, hasta)
            val ganancia = ventasRepository.gananciaEntreFechas(desde, hasta)
            val cantidadVentas = ventasRepository.cantidadVentasEntreFechas(desde, hasta)

            findViewById<TextView>(R.id.txtTotalVentas).text = formatoMoneda.format(totalVentas)
            findViewById<TextView>(R.id.txtGanancia).text = formatoMoneda.format(ganancia)

            val avgTicket = if (cantidadVentas > 0) totalVentas / cantidadVentas else 0.0
            findViewById<TextView>(R.id.txtAvgTicket).text = formatoMoneda.format(avgTicket)

            val topProductos = reportesRepository.topProductos(desde, hasta, limite = 5)
            topProductosAdapter.actualizar(topProductos)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun cargarGraficoRendimiento() {
        try {
            val dias = FechaUtils.ultimosNDias(7)
            val totales = ventasRepository.totalesPorDia(dias)
            val maximo = totales.values.maxOrNull()?.takeIf { it > 0 } ?: 1.0

            dias.forEachIndexed { index, dia ->
                if (index >= barIds.size) return@forEachIndexed
                val total = totales[dia] ?: 0.0
                val alturaDp = (16 + (total / maximo) * 104).toInt() // entre 16dp y 120dp
                val barView = findViewById<View>(barIds[index])
                val params = barView.layoutParams
                params.height = (alturaDp * resources.displayMetrics.density).toInt()
                barView.layoutParams = params

                findViewById<TextView>(lblIds[index]).text = FechaUtils.etiquetaDiaCorta(dia)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
                R.id.nav_reportes -> true
                else -> false
            }
        }
    }
}
