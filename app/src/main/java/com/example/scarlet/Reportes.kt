package com.example.scarlet

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
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
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import com.example.scarlet.util.Session
import com.example.scarlet.cart.CartManager


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


        val header = findViewById<View>(R.id.headerLayout)
        val bottomNavigation =
            findViewById<BottomNavigationView>(R.id.bottomNavigation)

        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.main)
        ) { _, insets ->

            val systemBars =
                insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                )

            // HEADER
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

            // MENÚ INFERIOR
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

        reportesRepository = ReportesRepository(this)
        ventasRepository = VentasRepository(this)

        configurarRecyclerView()
        setupFilters()
        setupBottomNavigation()
        setupNotifications()


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
