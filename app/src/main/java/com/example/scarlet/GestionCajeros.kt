package com.example.scarlet

import com.example.scarlet.util.NavegacionOrigen

import androidx.appcompat.app.AlertDialog

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.scarlet.adapter.CajerosAdapter
import com.example.scarlet.data.repository.CajeroInfo
import com.example.scarlet.data.repository.CuentaRepository
import com.example.scarlet.util.Session
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Pantalla de gestión de cuentas de Cajero. Sigue el mismo diseño y
 * funcionamiento que Proveedores (buscador, chips de filtro por estado,
 * activar/desactivar con switch inmediato, tarjeta con avatar).
 */
class GestionCajeros : AppCompatActivity() {

    private lateinit var cuentaRepository: CuentaRepository
    private lateinit var adapter: CajerosAdapter

    private lateinit var recyclerCajeros: RecyclerView
    private lateinit var edtBuscar: EditText
    private lateinit var filtroTodos: TextView
    private lateinit var filtroActivos: TextView
    private lateinit var filtroInactivos: TextView
    private lateinit var tvTotalCajeros: TextView
    private lateinit var txtVacio: TextView

    // Filtro de estado actualmente seleccionado: null = todos
    private var filtroEstadoActual: String? = null
    private var textoBusquedaActual: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gestion_cajeros)
        val header = findViewById<View>(R.id.headerLayout)

        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.headerLayout)
        ) { _, insets ->

            val systemBars =
                insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                )

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

            insets
        }

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { NavegacionOrigen.volverAOrigen(this) }

        if (!Session.esAdmin) {
            Toast.makeText(this, "Acceso solo para administradores", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        cuentaRepository = CuentaRepository(this)

        vincularVistas()
        configurarRecyclerView()
        configurarBuscador()
        configurarFiltros()
        configurarBotones()

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
        cargarCajeros()
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
    // =============================================
    // VISTAS
    // =============================================

    private fun vincularVistas() {
        recyclerCajeros = findViewById(R.id.recyclerCajeros)
        edtBuscar = findViewById(R.id.edtBuscarCajero)
        filtroTodos = findViewById(R.id.filtroTodos)
        filtroActivos = findViewById(R.id.filtroActivos)
        filtroInactivos = findViewById(R.id.filtroInactivos)
        tvTotalCajeros = findViewById(R.id.tvTotalCajeros)
        txtVacio = findViewById(R.id.txtVacioCajeros)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { NavegacionOrigen.volverAOrigen(this) }
    }

    private fun configurarRecyclerView() {
        adapter = CajerosAdapter(
            cajeros = emptyList(),
            onEditar = { cajero -> abrirEdicion(cajero) },
            onEstadoCambiado = { cajero, activo -> cambiarEstadoCajero(cajero, activo) }
        )
        recyclerCajeros.layoutManager = LinearLayoutManager(this)
        recyclerCajeros.adapter = adapter
    }

    private fun configurarBuscador() {
        edtBuscar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                textoBusquedaActual = s?.toString()?.trim() ?: ""
                cargarCajeros()
            }
        })
    }

    private fun configurarFiltros() {
        filtroTodos.setOnClickListener {
            filtroEstadoActual = null
            marcarFiltroSeleccionado(filtroTodos)
            cargarCajeros()
        }
        filtroActivos.setOnClickListener {
            filtroEstadoActual = "ACTIVO"
            marcarFiltroSeleccionado(filtroActivos)
            cargarCajeros()
        }
        filtroInactivos.setOnClickListener {
            filtroEstadoActual = "INACTIVO"
            marcarFiltroSeleccionado(filtroInactivos)
            cargarCajeros()
        }
    }

    private fun marcarFiltroSeleccionado(seleccionado: TextView) {
        val todos = listOf(filtroTodos, filtroActivos, filtroInactivos)
        todos.forEach { chip ->
            if (chip == seleccionado) {
                chip.setBackgroundResource(R.drawable.bg_category_selected)
                chip.setTextColor(Color.WHITE)
            } else {
                chip.setBackgroundResource(R.drawable.bg_category_unselected)
                chip.setTextColor(Color.parseColor("#777777"))
            }
        }
    }

    private fun configurarBotones() {
        findViewById<LinearLayout>(R.id.btnNuevoCajero).setOnClickListener {
            startActivity(Intent(this, FormularioCajero::class.java))
        }
    }

    // =============================================
    // CARGAR / FILTRAR LISTA
    // =============================================

    private fun cargarCajeros() {
        val lista = cuentaRepository.listarCajeros(
            filtroEstado = filtroEstadoActual,
            busqueda = textoBusquedaActual.ifBlank { null }
        )
        adapter.actualizar(lista)

        val (activos, inactivos) = cuentaRepository.contarCajerosPorEstado()
        val total = activos + inactivos
        filtroTodos.text = "Todos ($total)"
        filtroActivos.text = "Activos ($activos)"
        filtroInactivos.text = "Inactivos ($inactivos)"
        tvTotalCajeros.text = "${lista.size} en total"

        txtVacio.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
        recyclerCajeros.visibility = if (lista.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun cambiarEstadoCajero(cajero: CajeroInfo, activo: Boolean) {
        val exito = cuentaRepository.cambiarEstadoCajero(cajero.idCuenta, activo)
        if (!exito) {
            Toast.makeText(this, "No se pudo actualizar la cuenta", Toast.LENGTH_SHORT).show()
        }
        cargarCajeros()
    }

    private fun abrirEdicion(cajero: CajeroInfo) {
        val intent = Intent(this, FormularioCajero::class.java)
        intent.putExtra(FormularioCajero.EXTRA_ID_CUENTA, cajero.idCuenta)
        intent.putExtra(FormularioCajero.EXTRA_ID_PERSONA, cajero.idPersona)
        intent.putExtra(FormularioCajero.EXTRA_NOMBRES, cajero.nombres)
        intent.putExtra(FormularioCajero.EXTRA_APELLIDOS, cajero.apellidos)
        intent.putExtra(FormularioCajero.EXTRA_CI, cajero.ci)
        intent.putExtra(FormularioCajero.EXTRA_TELEFONO, cajero.telefono)
        intent.putExtra(FormularioCajero.EXTRA_USUARIO, cajero.usuario)
        startActivity(intent)
    }
}