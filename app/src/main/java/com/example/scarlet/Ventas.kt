package com.example.scarlet

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.ImageView
import android.widget.Toast
import android.view.ViewGroup
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

        val fechaLegible = SimpleDateFormat("EEEE d 'de' MMMM, yyyy", Locale("es", "ES")).format(Date())
        findViewById<TextView>(R.id.txtFechaHoy).text =
            fechaLegible.replaceFirstChar { it.uppercase() }
    }

    override fun onResume() {
        super.onResume()
        cargarVentas()
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
                NumberFormat.getCurrencyInstance(Locale.US).format(totalHoy)
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
