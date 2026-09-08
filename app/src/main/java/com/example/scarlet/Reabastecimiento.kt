package com.example.scarlet

import com.example.scarlet.util.NavegacionOrigen

import androidx.appcompat.app.AlertDialog

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.scarlet.adapter.ComprasAdapter
import com.example.scarlet.data.repository.ComprasRepository
import com.example.scarlet.util.Session

class Reabastecimiento : AppCompatActivity() {

    private lateinit var comprasRepository: ComprasRepository
    private lateinit var recyclerCompras: RecyclerView
    private lateinit var adapter: ComprasAdapter
    private lateinit var txtVacio: TextView

    private var filtroActual: String = "Todas"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reabastecimiento)
        findViewById<ImageView>(R.id.btnBack).setOnClickListener { NavegacionOrigen.volverAOrigen(this) }

        if (!Session.esAdmin) {
            Toast.makeText(this, "Acceso solo para administradores", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        comprasRepository = ComprasRepository(this)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { NavegacionOrigen.volverAOrigen(this) }

        findViewById<TextView>(R.id.btnNuevaCompra).setOnClickListener {
            startActivity(Intent(this, NuevaCompra::class.java))
        }

        recyclerCompras = findViewById(R.id.recyclerCompras)
        recyclerCompras.layoutManager = LinearLayoutManager(this)
        adapter = ComprasAdapter(emptyList()) { compra ->
            val intent = Intent(this, DetalleCompraActivity::class.java)
            intent.putExtra(DetalleCompraActivity.EXTRA_ID_COMPRA, compra.idCompra)
            startActivity(intent)
        }
        recyclerCompras.adapter = adapter

        txtVacio = findViewById(R.id.txtVacio)

        configurarTabs()

        // Si se abrió desde la alerta de stock bajo, arranca mostrando pendientes
        if (intent.getBooleanExtra(EXTRA_FILTRO_PENDIENTES, false)) {
            seleccionarTab(findViewById(R.id.tabPendientes), "PENDIENTE")
        }

        val imgMenu = findViewById<ImageView>(R.id.imgMenu)
        val sideMenu = findViewById<LinearLayout>(R.id.sideMenu)
        val menuOverlay = findViewById<View>(R.id.viewMenuOverlay)
        val menuProveedores = findViewById<TextView>(R.id.menuProveedores)
        val menuMiCuenta = findViewById<TextView>(R.id.menuMiCuenta)

        fun abrirMenu() {
            menuOverlay.visibility = View.VISIBLE
            sideMenu.visibility = View.VISIBLE
            resaltarItemMenuActual()
            // Se espera al siguiente frame para que sideMenu ya tenga su
            // ancho medido (si estaba GONE, width valía 0 y el menú no
            // se deslizaba desde fuera de la pantalla).
            sideMenu.post {
                sideMenu.translationX = -sideMenu.width.toFloat()
                sideMenu.animate().translationX(0f).setDuration(250).start()
            }
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
            NavegacionOrigen.cambiarModulo(this, MiCuenta::class.java)
        }
        // para caja
        findViewById<TextView>(R.id.menuCaja).setOnClickListener {
            cerrarMenu()
            NavegacionOrigen.cambiarModulo(this, CajaActivity::class.java)
        }
        // para proveedores
        menuProveedores.setOnClickListener {
            cerrarMenu()
            NavegacionOrigen.cambiarModulo(this, Proveedores::class.java)
        }
        // para categorías
        findViewById<TextView>(R.id.menuCategorias).setOnClickListener {
            cerrarMenu()
            NavegacionOrigen.cambiarModulo(this, CategoriasActivity::class.java)
        }
        // para inventario
        findViewById<TextView>(R.id.menuInventario).setOnClickListener {
            cerrarMenu()
            NavegacionOrigen.cambiarModulo(this, Inventario::class.java)
        }
        // para reabastecimiento / compras
        findViewById<TextView>(R.id.menuReabastecimiento).setOnClickListener {
            cerrarMenu()
            NavegacionOrigen.cambiarModulo(this, Reabastecimiento::class.java)
        }
        // para cuentas de cajero
        findViewById<TextView>(R.id.menuCajeros).setOnClickListener {
            cerrarMenu()
            NavegacionOrigen.cambiarModulo(this, GestionCajeros::class.java)
        }
        // Restringe accesos de gestión a solo el rol Administrador.
        if (!Session.esAdmin) {
            findViewById<TextView>(R.id.menuCategorias).visibility = View.GONE
            menuProveedores.visibility = View.GONE
            findViewById<TextView>(R.id.menuInventario).visibility = View.GONE
            findViewById<TextView>(R.id.menuReabastecimiento).visibility = View.GONE
            findViewById<TextView>(R.id.menuCajeros).visibility = View.GONE
        }
        // cerrar sesión
        findViewById<TextView>(R.id.menuSalir).setOnClickListener {
            cerrarMenu()
            AlertDialog.Builder(this)
                .setTitle("Cerrar sesión")
                .setMessage("¿Estás seguro de que deseas cerrar sesión?")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Salir") { _, _ ->
                    Session.cerrar()
                    com.example.scarlet.cart.CartManager.limpiar()
                    val intent = Intent(this, Login::class.java)
                    intent.flags =
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
                .show()
        }
    }

    override fun onResume() {
        super.onResume()
        cargarDatos()
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
    private fun configurarTabs() {
        val tabs = listOf(
            R.id.tabTodas to "Todas",
            R.id.tabPendientes to "PENDIENTE",
            R.id.tabRecibidas to "RECIBIDA",
            R.id.tabAnuladas to "ANULADA"
        )

        tabs.forEach { (id, estado) ->
            findViewById<TextView>(id).setOnClickListener { view ->
                seleccionarTab(view as TextView, estado)
            }
        }
    }

    private fun seleccionarTab(seleccionado: TextView, estado: String) {
        val ids = listOf(R.id.tabTodas, R.id.tabPendientes, R.id.tabRecibidas, R.id.tabAnuladas)
        ids.forEach { id ->
            val tab = findViewById<TextView>(id)
            if (tab.id == seleccionado.id) {
                tab.setBackgroundResource(R.drawable.bg_filter_selected)
                tab.setTextColor(0xFFFFFFFF.toInt())
            } else {
                tab.setBackgroundResource(R.drawable.bg_filter_unselected)
                tab.setTextColor(0xFF888888.toInt())
            }
        }
        filtroActual = estado
        cargarDatos()
    }

    private fun cargarDatos() {
        val stats = comprasRepository.obtenerEstadisticas()
        findViewById<TextView>(R.id.txtOrdenesAbiertas).text = stats.ordenesAbiertas.toString()
        findViewById<TextView>(R.id.txtPendientePago).text = ComprasAdapter.formatearBs(stats.pendientePago)
        findViewById<TextView>(R.id.txtTotalRecibido).text =
            ComprasAdapter.formatearBs(stats.totalRecibido) + "\n${stats.pedidosRecibidos} pedidos"

        val compras = comprasRepository.listarCompras(filtroActual)
        adapter.actualizarCompras(compras)

        txtVacio.visibility = if (compras.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        recyclerCompras.visibility = if (compras.isEmpty()) android.view.View.GONE else android.view.View.VISIBLE
    }

    companion object {
        const val EXTRA_FILTRO_PENDIENTES = "extra_filtro_pendientes"
    }
}