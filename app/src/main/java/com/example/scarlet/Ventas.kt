package com.example.scarlet

import com.example.scarlet.util.NavegacionOrigen

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.ImageView
import android.widget.Toast
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.scarlet.adapter.VentasAdapter
import com.example.scarlet.data.repository.CuentaRepository
import com.example.scarlet.data.repository.DetalleVentaRepository
import com.example.scarlet.data.repository.VentasRepository
import com.example.scarlet.util.FechaUtils
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
//
import androidx.appcompat.app.AlertDialog
import com.example.scarlet.util.Session
import com.example.scarlet.util.ImpuestosUtils
import com.example.scarlet.util.ComprobanteUtils
import com.example.scarlet.cart.CartManager


class Ventas : AppCompatActivity() {

    private lateinit var ventasRepository: VentasRepository
    private lateinit var detalleVentaRepository: DetalleVentaRepository
    private lateinit var cuentaRepository: CuentaRepository

    private lateinit var recyclerViewVentas: RecyclerView
    private lateinit var adapter: VentasAdapter
    private lateinit var tvSinVentas: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sales)

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
        // Restringe accesos de gestión a solo el rol Administrador.
        if (!Session.esAdmin) {
            findViewById<TextView>(R.id.menuCategorias).visibility = View.GONE
            menuProveedores.visibility = View.GONE
            findViewById<TextView>(R.id.menuInventario).visibility = View.GONE
            findViewById<TextView>(R.id.menuReabastecimiento).visibility = View.GONE
            findViewById<TextView>(R.id.menuCajeros).visibility = View.GONE
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

        val header = findViewById<android.view.View>(R.id.headerLayout)
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

        ventasRepository = VentasRepository(this)
        detalleVentaRepository = DetalleVentaRepository(this)
        cuentaRepository = CuentaRepository(this)

        tvSinVentas = findViewById(R.id.tvSinVentas)
        recyclerViewVentas = findViewById(R.id.recyclerViewVentas)
        recyclerViewVentas.layoutManager = LinearLayoutManager(this)
        recyclerViewVentas.isNestedScrollingEnabled = false

        adapter = VentasAdapter(
            ventas = emptyList(),
            obtenerLineasDeVenta = { idVenta ->
                detalleVentaRepository.listarPorVentaConNombre(idVenta).map { (nombre, detalle) ->
                    "${detalle.cantidad}x $nombre"
                }
            },
            // NUEVO: al tocar cualquier venta del historial (sin importar su
            // fecha/hora) se abre su factura completa, con opción a imprimirla.
            onClickVenta = { idVenta -> mostrarFacturaVenta(idVenta) }
        )
        recyclerViewVentas.adapter = adapter

        cargarInformacionUsuario()
        setupBottomNavigation()
        setupNotifications()
        setupCarritoYPerfil()

        val fechaLegible = SimpleDateFormat("EEEE d 'de' MMMM, yyyy", Locale("es", "ES")).format(Date())
        findViewById<TextView>(R.id.txtFechaHoy).text =
            fechaLegible.replaceFirstChar { it.uppercase() }
    }

    override fun onResume() {
        super.onResume()
        cargarVentas()
        actualizarBadgeCarrito()
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
            tvCartBadge.visibility = View.VISIBLE
        } else {
            tvCartBadge.visibility = View.GONE
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
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun cargarVentas() {
        try {
            // Historial "por rol": cada cajero (y el propio admin) ve solo
            // las ventas que él mismo registró, no las de todas las cuentas.
            val idCuenta = Session.idCuenta
            val ventas = ventasRepository.listarResumen(idCuenta = idCuenta)
            adapter.actualizar(ventas)
            tvSinVentas.visibility = if (ventas.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            recyclerViewVentas.visibility = if (ventas.isEmpty()) android.view.View.GONE else android.view.View.VISIBLE

            val (desde, hasta) = FechaUtils.rangoParaFiltro("Día")
            val totalHoy = ventasRepository.totalEntreFechas(desde, hasta, idCuenta)
            findViewById<TextView>(R.id.txtRevenueHoy).text =
                "Bs " + String.format(Locale("es", "BO"), "%,.2f", totalHoy)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Reconstruye y muestra la factura completa de CUALQUIER venta pasada
     * (subtotal, IVA 13%, IT 3% y total), con opción de imprimirla, sin
     * importar la fecha/hora en la que se realizó.
     *
     * La BD solo guarda el total final con impuestos incluidos, así que el
     * desglose se reconstruye con [ImpuestosUtils.desdeTotalConImpuestos].
     */
    private fun mostrarFacturaVenta(idVenta: Int) {
        try {
            val factura = ventasRepository.obtenerParaFactura(idVenta)
            if (factura == null) {
                Toast.makeText(this, "No se encontró la venta #$idVenta", Toast.LENGTH_SHORT).show()
                return
            }

            val lineas = detalleVentaRepository.listarPorVentaConNombre(idVenta)
            val items = lineas.map { (nombre, detalle) ->
                Triple(nombre, detalle.cantidad, detalle.precio * detalle.cantidad)
            }

            val desglose = ImpuestosUtils.desdeTotalConImpuestos(factura.total)

            val vista = LayoutInflater.from(this).inflate(R.layout.dialog_recibo_venta, null)
            vista.findViewById<TextView>(R.id.txtNumeroReciboVenta).text = "Venta #${factura.idVenta} · Completada"
            vista.findViewById<TextView>(R.id.txtFechaReciboVenta).text = "Fecha: ${factura.fecha}"
            vista.findViewById<TextView>(R.id.txtClienteReciboVenta).text = "Cliente: ${factura.nombreCliente}"
            vista.findViewById<TextView>(R.id.txtCajeroReciboVenta).text = "Atendido por: ${factura.nombreCajero}"
            vista.findViewById<TextView>(R.id.txtMetodoPagoReciboVenta).text = "Método de pago: ${factura.metodoPago}"
            vista.findViewById<TextView>(R.id.txtSubtotalReciboVenta).text =
                "Bs " + String.format(Locale("es", "BO"), "%,.2f", desglose.subtotal)
            vista.findViewById<TextView>(R.id.txtIvaReciboVenta).text =
                "Bs " + String.format(Locale("es", "BO"), "%,.2f", desglose.iva)
            vista.findViewById<TextView>(R.id.txtItReciboVenta).text =
                "Bs " + String.format(Locale("es", "BO"), "%,.2f", desglose.it)
            vista.findViewById<TextView>(R.id.txtTotalReciboVenta).text =
                "Bs " + String.format(Locale("es", "BO"), "%,.2f", desglose.total)

            val llLineas = vista.findViewById<LinearLayout>(R.id.llLineasRecibo)
            items.forEach { (nombre, cantidad, subtotalLinea) ->
                val fila = LayoutInflater.from(this).inflate(R.layout.item_linea_recibo, llLineas, false)
                val precioUnit = if (cantidad > 0) subtotalLinea / cantidad else subtotalLinea
                fila.findViewById<TextView>(R.id.txtNombreLineaRecibo).text = nombre
                fila.findViewById<TextView>(R.id.txtCantidadLineaRecibo).text = "x$cantidad"
                fila.findViewById<TextView>(R.id.txtPrecioUnitLineaRecibo).text =
                    "Bs " + String.format(Locale("es", "BO"), "%,.2f", precioUnit) + " c/u"
                fila.findViewById<TextView>(R.id.txtSubtotalLineaRecibo).text =
                    "Bs " + String.format(Locale("es", "BO"), "%,.2f", subtotalLinea)
                llLineas.addView(fila)
            }

            val dialog = AlertDialog.Builder(this)
                .setView(vista)
                .setCancelable(true)
                .create()
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            vista.findViewById<TextView>(R.id.btnImprimirReciboVenta).setOnClickListener {
                ComprobanteUtils.imprimirVenta(
                    context = this,
                    idVenta = factura.idVenta,
                    fecha = factura.fecha,
                    cajero = factura.nombreCajero,
                    cliente = factura.nombreCliente,
                    metodoPago = factura.metodoPago,
                    items = items,
                    subtotal = desglose.subtotal,
                    iva = desglose.iva,
                    impuestoIt = desglose.it,
                    total = desglose.total
                )
            }

            // En el historial esto es solo consulta: el botón "Continuar"
            // del checkout aquí simplemente cierra el diálogo.
            vista.findViewById<TextView>(R.id.btnContinuarReciboVenta)?.apply {
                text = "Cerrar"
                setOnClickListener { dialog.dismiss() }
            }

            dialog.show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "No se pudo abrir la factura de la venta #$idVenta", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_ventas

        bottomNav.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_productos -> {
                    startActivity(Intent(this, Productos::class.java))
                    finish()
                    true
                }
                R.id.nav_ventas -> true
                R.id.nav_reportes -> {
                    startActivity(Intent(this, Reportes::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupNotifications() {
        com.example.scarlet.util.AlertasUtils.configurar(this)
    }
}