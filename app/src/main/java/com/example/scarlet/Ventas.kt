package com.example.scarlet

import android.content.Intent
import android.os.Bundle
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

        adapter = VentasAdapter(emptyList()) { idVenta ->
            detalleVentaRepository.listarPorVentaConNombre(idVenta).map { (nombre, detalle) ->
                "${detalle.cantidad}x $nombre"
            }
        }
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

    // El ícono de carrito y el de perfil existían en el layout pero nunca
    // tenían onClickListener (botones "muertos"). Se conectan aquí.
    private fun setupCarritoYPerfil() {
        findViewById<ImageView>(R.id.imgCarrito).setOnClickListener {
            if (CartManager.estaVacio()) {
                Toast.makeText(this, "Tu carrito está vacío. Agrega productos primero.", Toast.LENGTH_SHORT).show()
            } else {
                startActivity(Intent(this, Shopping::class.java))
            }
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
            val ventas = ventasRepository.listarResumen()
            adapter.actualizar(ventas)
            tvSinVentas.visibility = if (ventas.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            recyclerViewVentas.visibility = if (ventas.isEmpty()) android.view.View.GONE else android.view.View.VISIBLE

            val (desde, hasta) = FechaUtils.rangoParaFiltro("Día")
            val totalHoy = ventasRepository.totalEntreFechas(desde, hasta)
            findViewById<TextView>(R.id.txtRevenueHoy).text =
                "Bs " + String.format(Locale("es", "BO"), "%,.2f", totalHoy)
        } catch (e: Exception) {
            e.printStackTrace()
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