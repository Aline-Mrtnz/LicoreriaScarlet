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
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
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
}
