package com.example.scarlet

import com.example.scarlet.util.NavegacionOrigen

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
import com.example.scarlet.data.repository.CuentaRepository
import com.example.scarlet.data.repository.VentasRepository
import com.example.scarlet.util.FechaUtils
import com.example.scarlet.util.ImpuestosUtils
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.text.NumberFormat
import java.util.Locale
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import com.example.scarlet.util.Session
import com.example.scarlet.util.ComprobanteUtils
import com.example.scarlet.cart.CartManager
import com.example.scarlet.data.model.TopProducto


class Reportes : AppCompatActivity() {

    private lateinit var recyclerViewTopProductos: RecyclerView
    private lateinit var topProductosAdapter: TopProductosAdapter

    private lateinit var reportesRepository: ReportesRepository
    private lateinit var ventasRepository: VentasRepository
    private lateinit var cuentaRepository: CuentaRepository

    private val formatoMoneda = java.text.DecimalFormat("Bs #,##0.00")

    // Ids de las 7 barras/etiquetas del gráfico "Rendimiento" en el mismo orden
    private val barIds = listOf(R.id.barDia1, R.id.barDia2, R.id.barDia3, R.id.barDia4, R.id.barDia5, R.id.barDia6, R.id.barDia7)
    private val lblIds = listOf(R.id.lblDia1, R.id.lblDia2, R.id.lblDia3, R.id.lblDia4, R.id.lblDia5, R.id.lblDia6, R.id.lblDia7)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_reports)

        // ANTES esta pantalla era 100% exclusiva de Administrador (bloqueaba
        // a cualquier cajero que la abriera desde el menú inferior). Ahora
        // cualquier cuenta logueada puede entrar, pero cada quien ve SOLO
        // sus propias estadísticas/reportes: el filtrado por rol se hace
        // más abajo, en updateDataForFilter()/cargarGraficoRendimiento(),
        // usando `idCuentaParaReporte()`. Solo el Administrador tiene además
        // el botón "Ver reporte general" para juntar todas las cuentas
        // (ya que en este sistema solo puede existir un Administrador).
        if (!Session.estaLogueado) {
            Toast.makeText(this, "Debes iniciar sesión", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val imgMenu = findViewById<ImageView>(R.id.imgMenu)
        val sideMenu = findViewById<LinearLayout>(R.id.sideMenu)
        val menuOverlay = findViewById<View>(R.id.viewMenuOverlay)
        val menuProveedores = findViewById<TextView>(R.id.menuProveedores)
        val menuMiCuenta = findViewById<TextView>(R.id.menuMiCuenta)

        fun abrirMenu() {
            menuOverlay.visibility = View.VISIBLE
            sideMenu.visibility = View.VISIBLE
            sideMenu.translationX = -sideMenu.width.toFloat()
            sideMenu.animate().translationX(0f).setDuration(250).start()
            resaltarItemMenuActual()
        }

        fun cerrarMenu() {
            sideMenu.animate()
                .translationX(-sideMenu.width.toFloat())
                .setDuration(200)
                .withEndAction {
                    sideMenu.visibility = View.GONE
                    menuOverlay.visibility = View.GONE
                }
                .start()
        }

        imgMenu.setOnClickListener {
            if (sideMenu.visibility == View.GONE) abrirMenu() else cerrarMenu()
        }

        // Cualquier toque fuera del menú (en el resto de la pantalla) lo cierra
        menuOverlay.setOnClickListener { cerrarMenu() }
        // para el mi cuenta
        menuMiCuenta.setOnClickListener {
            cerrarMenu()
            NavegacionOrigen.abrirModulo(this, MiCuenta::class.java)
        }
        // para caja
        findViewById<TextView>(R.id.menuCaja).setOnClickListener {
            cerrarMenu()
            NavegacionOrigen.abrirModulo(this, CajaActivity::class.java)
        }
        // para proveedores (antes no tenía listener: era inalcanzable)
        menuProveedores.setOnClickListener {
            cerrarMenu()
            NavegacionOrigen.abrirModulo(this, Proveedores::class.java)
        }
        // para categorías
        findViewById<TextView>(R.id.menuCategorias).setOnClickListener {
            cerrarMenu()
            NavegacionOrigen.abrirModulo(this, CategoriasActivity::class.java)
        }
        // para inventario (existía en el layout pero sin listener: era inalcanzable)
        findViewById<TextView>(R.id.menuInventario).setOnClickListener {
            cerrarMenu()
            NavegacionOrigen.abrirModulo(this, Inventario::class.java)
        }
        // para reabastecimiento / compras
        findViewById<TextView>(R.id.menuReabastecimiento).setOnClickListener {
            cerrarMenu()
            NavegacionOrigen.abrirModulo(this, Reabastecimiento::class.java)
        }
        // para cuentas de cajero
        findViewById<TextView>(R.id.menuCajeros).setOnClickListener {
            cerrarMenu()
            NavegacionOrigen.abrirModulo(this, GestionCajeros::class.java)
        }
        // cerra sesion

        findViewById<TextView>(R.id.menuSalir).setOnClickListener {

            cerrarMenu()

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
        cuentaRepository = CuentaRepository(this)

        configurarRecyclerView()
        setupFilters()
        setupVistaGeneral()
        setupComprobanteReporte()
        setupBottomNavigation()
        setupNotifications()
        cargarInformacionUsuario()
        setupCarritoYPerfil()


        updateDataForFilter("Día")
        cargarGraficoRendimiento()
    }

    override fun onResume() {
        super.onResume()
        updateDataForFilter(filtroActual)
        cargarGraficoRendimiento()
        actualizarBadgeCarrito()
    }

    private fun cargarInformacionUsuario() {
        val txtNombre = findViewById<TextView>(R.id.txtNombre)
        val txtRol = findViewById<TextView>(R.id.txtRol)
        try {
            val usuarioInfo = cuentaRepository.obtenerUsuarioActual()
            if (usuarioInfo != null) {
                txtNombre.text = "${usuarioInfo.nombres} ${usuarioInfo.apellidos}"
                txtRol.text = "●  ${usuarioInfo.nombreRol}"
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // El ícono de carrito y el de perfil existían en el layout pero nunca
    // tenían onClickListener (botones "muertos"). Se conectan aquí.
    private fun setupCarritoYPerfil() {
        val imgCarritoReportes = findViewById<ImageView>(R.id.imgCarrito)
        imgCarritoReportes.setOnClickListener {
            com.example.scarlet.util.CarritoUtils.manejarClick(this, imgCarritoReportes)
        }

        findViewById<ImageView>(R.id.imgPerfil).setOnClickListener {
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

        actualizarBadgeCarrito()
    }

    private fun actualizarBadgeCarrito() {
        val tvCartBadge = findViewById<TextView>(R.id.tvCartBadge)
        val total = CartManager.totalItems()
        if (total > 0) {
            tvCartBadge.text = if (total > 99) "99+" else total.toString()
            tvCartBadge.visibility = android.view.View.VISIBLE
        } else {
            tvCartBadge.visibility = android.view.View.GONE
        }
    }

    private var filtroActual = "Día"

    // Reporte "por rol" (default): cada cajero/admin ve solo sus propias
    // ventas. Solo el Administrador puede activar la vista general, que
    // junta las ventas de TODAS las cuentas (todos los cajeros + admin).
    private var vistaGeneral = false

    /** Cuenta por la que se debe filtrar el reporte actual, o null = todas (vista general). */
    private fun idCuentaParaReporte(): Int? = if (vistaGeneral) null else Session.idCuenta

    private fun textoAlcanceReporte(): String =
        if (vistaGeneral) "Vista general · todas las cuentas"
        else "Cuenta: ${Session.nombreCompleto} (${Session.rol})"

    // Datos del último periodo consultado, guardados para poder generar el
    // comprobante del reporte con los mismos números que se ven en pantalla.
    private var reporteDesde: String = ""
    private var reporteHasta: String = ""
    private var reporteTotalVentas: Double = 0.0
    private var reporteSubtotalVentas: Double = 0.0
    private var reporteIva: Double = 0.0
    private var reporteIt: Double = 0.0
    private var reporteGanancia: Double = 0.0
    private var reporteCantidadVentas: Int = 0
    private var reporteTopProductos: List<TopProducto> = emptyList()
    private var reporteRendimiento: List<Pair<String, Double>> = emptyList()

    private fun configurarRecyclerView() {
        recyclerViewTopProductos = findViewById(R.id.recyclerViewTopProductos)
        recyclerViewTopProductos.layoutManager = LinearLayoutManager(this)
        recyclerViewTopProductos.isNestedScrollingEnabled = false

        topProductosAdapter = TopProductosAdapter(emptyList())
        recyclerViewTopProductos.adapter = topProductosAdapter
    }

    /**
     * El botón "Ver reporte general" SOLO existe para Administrador (en este
     * sistema solo puede haber un admin). Al tocarlo alterna entre ver solo
     * sus propias ventas y ver las de todos los cajeros + las suyas.
     */
    private fun setupVistaGeneral() {
        val btnVistaGeneral = findViewById<TextView>(R.id.btnVistaGeneral)

        if (!Session.esAdmin) {
            btnVistaGeneral.visibility = View.GONE
            return
        }

        btnVistaGeneral.visibility = View.VISIBLE
        actualizarEstiloVistaGeneral(btnVistaGeneral)

        btnVistaGeneral.setOnClickListener {
            vistaGeneral = !vistaGeneral
            actualizarEstiloVistaGeneral(btnVistaGeneral)
            updateDataForFilter(filtroActual)
            cargarGraficoRendimiento()
        }
    }

    private fun actualizarEstiloVistaGeneral(boton: TextView) {
        if (vistaGeneral) {
            boton.text = "👤  Ver solo mis ventas"
            boton.setBackgroundResource(R.drawable.bg_filter_selected)
        } else {
            boton.text = "👥  Ver reporte general (todos los cajeros)"
            boton.setBackgroundResource(R.drawable.bg_filter_unselected)
        }
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
                // IMPORTANTE: antes el gráfico de "Rendimiento" no se
                // refrescaba al cambiar de filtro (por eso siempre mostraba
                // lo mismo). Ahora que sí depende del filtro, hay que
                // recargarlo también aquí.
                cargarGraficoRendimiento()
            }
        }
    }

    private fun updateDataForFilter(filter: String) {
        filtroActual = filter
        try {
            val (desde, hasta) = FechaUtils.rangoParaFiltro(filter)
            val idCuenta = idCuentaParaReporte()

            // El total que se ve aquí YA incluye impuestos (así se guarda
            // ahora en ventas.total, ver VentasRepository). Para desglosar
            // subtotal/IVA/IT del periodo se revierte la fórmula.
            val totalVentas = ventasRepository.totalEntreFechas(desde, hasta, idCuenta)
            val desgloseImpuestos = ImpuestosUtils.desdeTotalConImpuestos(totalVentas)
            val ganancia = ventasRepository.gananciaEntreFechas(desde, hasta, idCuenta)
            val cantidadVentas = ventasRepository.cantidadVentasEntreFechas(desde, hasta, idCuenta)

            findViewById<TextView>(R.id.txtTotalVentas).text = formatoMoneda.format(totalVentas)
            findViewById<TextView>(R.id.txtGanancia).text = formatoMoneda.format(ganancia)

            val avgTicket = if (cantidadVentas > 0) totalVentas / cantidadVentas else 0.0
            findViewById<TextView>(R.id.txtAvgTicket).text = formatoMoneda.format(avgTicket)

            val topProductos = reportesRepository.topProductos(desde, hasta, limite = 5, idCuenta = idCuenta)
            topProductosAdapter.actualizar(topProductos)

            cargarTendencias(filter, totalVentas, ganancia, idCuenta)
            cargarTarjetasSecundarias(desde, hasta, cantidadVentas, idCuenta)

            // Guardamos los datos reales del periodo consultado para el comprobante.
            reporteDesde = desde
            reporteHasta = hasta
            reporteTotalVentas = totalVentas
            reporteSubtotalVentas = desgloseImpuestos.subtotal
            reporteIva = desgloseImpuestos.iva
            reporteIt = desgloseImpuestos.it
            reporteGanancia = ganancia
            reporteCantidadVentas = cantidadVentas
            reporteTopProductos = topProductos
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Antes las flechitas "▲ +12.5%" / "▲ +8.2%" de Total Ventas y Ganancia
     * eran texto fijo en el XML. Ahora se comparan contra el periodo
     * inmediatamente anterior de la misma duración
     * ([FechaUtils.rangoAnteriorParaFiltro]) y se calcula el % real.
     */
    private fun cargarTendencias(filter: String, totalVentasActual: Double, gananciaActual: Double, idCuenta: Int?) {
        val (desdeAnterior, hastaAnterior) = FechaUtils.rangoAnteriorParaFiltro(filter)
        val totalVentasAnterior = ventasRepository.totalEntreFechas(desdeAnterior, hastaAnterior, idCuenta)
        val gananciaAnterior = ventasRepository.gananciaEntreFechas(desdeAnterior, hastaAnterior, idCuenta)

        pintarTendencia(findViewById(R.id.txtTendenciaVentas), totalVentasAnterior, totalVentasActual)
        pintarTendencia(findViewById(R.id.txtTendenciaGanancia), gananciaAnterior, gananciaActual)
    }

    private fun pintarTendencia(view: TextView, anterior: Double, actual: Double) {
        val cambio = when {
            anterior > 0 -> ((actual - anterior) / anterior) * 100.0
            actual > 0 -> 100.0
            else -> 0.0
        }
        val subio = cambio >= 0
        val flecha = if (subio) "▲" else "▼"
        val signo = if (subio) "+" else ""
        view.text = "$flecha $signo${String.format(Locale("es", "BO"), "%.1f", cambio)}%"
        view.setTextColor(if (subio) 0xFF4CAF50.toInt() else 0xFFE53935.toInt())
    }

    /**
     * Llena con datos reales las tarjetas que antes tenían valores fijos
     * en el XML: Artículos/Venta, Top Categoría, Nuevos clientes y
     * Category Split (antes "Conversión 12.5%", "Whisky", "48" y el
     * desglose Whisky/Tequila/Wine/Others hardcodeado).
     */
    private fun cargarTarjetasSecundarias(desde: String, hasta: String, cantidadVentas: Int, idCuenta: Int?) {
        // Artículos por venta = unidades vendidas / cantidad de ventas.
        val unidadesVendidas = ventasRepository.sumaCantidadEntreFechas(desde, hasta, idCuenta)
        val articulosPorVenta = if (cantidadVentas > 0) unidadesVendidas.toDouble() / cantidadVentas else 0.0
        findViewById<TextView>(R.id.txtArticulosPorVenta).text =
            String.format(Locale("es", "BO"), "%.1f", articulosPorVenta)

        // Split de ventas por categoría (top 3 + "Otros").
        val split = reportesRepository.splitPorCategoria(desde, hasta, idCuenta)
        val totalSplit = split.sumOf { it.monto }

        findViewById<TextView>(R.id.txtTopCategoria).text = split.firstOrNull()?.nombre ?: "-"

        val filas = listOf(
            Triple(R.id.rowCatSplit1, R.id.txtCatSplit1Label, R.id.txtCatSplit1Value),
            Triple(R.id.rowCatSplit2, R.id.txtCatSplit2Label, R.id.txtCatSplit2Value),
            Triple(R.id.rowCatSplit3, R.id.txtCatSplit3Label, R.id.txtCatSplit3Value)
        )
        filas.forEachIndexed { index, (idRow, idLabel, idValue) ->
            val fila = findViewById<View>(idRow)
            val categoria = split.getOrNull(index)
            if (categoria != null && totalSplit > 0) {
                fila.visibility = View.VISIBLE
                findViewById<TextView>(idLabel).text = categoria.nombre
                val porcentaje = (categoria.monto / totalSplit) * 100.0
                findViewById<TextView>(idValue).text = String.format(Locale("es", "BO"), "%.0f%%", porcentaje)
            } else {
                fila.visibility = View.GONE
            }
        }

        val rowOtros = findViewById<View>(R.id.rowCatSplitOtros)
        if (split.size > 3 && totalSplit > 0) {
            val montoOtros = split.drop(3).sumOf { it.monto }
            val porcentajeOtros = (montoOtros / totalSplit) * 100.0
            findViewById<TextView>(R.id.txtCatSplitOtrosValue).text =
                String.format(Locale("es", "BO"), "%.0f%%", porcentajeOtros)
            rowOtros.visibility = View.VISIBLE
        } else {
            rowOtros.visibility = View.GONE
        }

        // Nuevos clientes: primera compra (dentro del alcance filtrado) cae en este periodo.
        val nuevosClientes = reportesRepository.clientesNuevosEntreFechas(desde, hasta, idCuenta)
        findViewById<TextView>(R.id.txtNuevosClientes).text = nuevosClientes.toString()
    }

    private fun setupComprobanteReporte() {
        findViewById<TextView>(R.id.btnComprobanteReporte).setOnClickListener {
            val ticketPromedio = if (reporteCantidadVentas > 0) reporteTotalVentas / reporteCantidadVentas else 0.0
            // El comprobante ahora imprime TODO lo que se ve en la pantalla
            // de Reportes: subtotal/IVA/IT, ganancia, cantidad de ventas,
            // ticket promedio, el detalle del gráfico "Rendimiento" y el top
            // de productos. Antes solo salían ganancia/cantidad/ticket/top.
            ComprobanteUtils.imprimirReporte(
                context = this,
                periodo = filtroActual,
                desde = reporteDesde,
                hasta = reporteHasta,
                alcance = textoAlcanceReporte(),
                totalVentas = reporteTotalVentas,
                subtotalVentas = reporteSubtotalVentas,
                iva = reporteIva,
                it = reporteIt,
                ganancia = reporteGanancia,
                cantidadVentas = reporteCantidadVentas,
                ticketPromedio = ticketPromedio,
                rendimiento = reporteRendimiento,
                topProductos = reporteTopProductos
            )
        }
    }

    /**
     * ANTES este gráfico llamaba siempre a `ultimosNDias(7)`, así que se
     * veía exactamente igual sin importar si arriba elegías Día, Semana,
     * Mes o Año. Ahora usa `FechaUtils.bucketsParaGrafico(filtroActual)`,
     * que arma 7 tramos de tiempo distintos según el filtro (horas de hoy,
     * días de la semana, tramos del mes o meses del año), y respeta también
     * el filtro de cuenta (reporte por rol / vista general).
     */
    private fun cargarGraficoRendimiento() {
        try {
            val buckets = FechaUtils.bucketsParaGrafico(filtroActual)
            val idCuenta = idCuentaParaReporte()
            val totalesPorBucket = ventasRepository.totalesPorBucket(buckets, idCuenta)
            val maximo = totalesPorBucket.values.maxOrNull()?.takeIf { it > 0 } ?: 1.0

            findViewById<TextView>(R.id.txtRendimientoSubtitulo).text = when (filtroActual) {
                "Día" -> "Hoy, por franja horaria"
                "Mes" -> "Últimos 28 días"
                "Año" -> "Últimos 7 meses"
                else -> "Últimos 7 días"
            }

            buckets.forEachIndexed { index, (etiqueta, _, _) ->
                if (index >= barIds.size) return@forEachIndexed
                val total = totalesPorBucket[etiqueta] ?: 0.0
                val alturaDp = (16 + (total / maximo) * 104).toInt() // entre 16dp y 120dp
                val barView = findViewById<View>(barIds[index])
                val params = barView.layoutParams
                params.height = (alturaDp * resources.displayMetrics.density).toInt()
                barView.layoutParams = params

                findViewById<TextView>(lblIds[index]).text = etiqueta
            }

            reporteRendimiento = buckets.map { (etiqueta, _, _) -> etiqueta to (totalesPorBucket[etiqueta] ?: 0.0) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    private fun resaltarItemMenuActual() {
        // Mapea cada item del menú con la Activity a la que navega.
        // null = no navega a otra Activity (ej. "Salir"), nunca se resalta.
        val items = listOf(
            findViewById<TextView>(R.id.menuMiCuenta) to MiCuenta::class.java,
            findViewById<TextView>(R.id.menuCaja) to CajaActivity::class.java,
            findViewById<TextView>(R.id.menuCategorias) to CategoriasActivity::class.java,
            findViewById<TextView>(R.id.menuProveedores) to Proveedores::class.java,
            findViewById<TextView>(R.id.menuInventario) to Inventario::class.java,
            findViewById<TextView>(R.id.menuReabastecimiento) to Reabastecimiento::class.java,
            findViewById<TextView>(R.id.menuCajeros) to GestionCajeros::class.java
        )

        items.forEach { (item, clase) ->
            val esActual = clase == this::class.java
            item.setBackgroundResource(
                if (esActual) R.drawable.bg_menu_item_selected else android.R.color.transparent
            )
            item.setTextColor(if (esActual) 0xFFFF3B16.toInt() else 0xFFCCCCCC.toInt())
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
    }*/
    private fun setupNotifications() {
        com.example.scarlet.util.AlertasUtils.configurar(this)
    }
}