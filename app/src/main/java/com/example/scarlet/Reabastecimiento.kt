package com.example.scarlet

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
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

        if (!Session.esAdmin) {
            Toast.makeText(this, "Acceso solo para administradores", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        comprasRepository = ComprasRepository(this)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

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
    }

    override fun onResume() {
        super.onResume()
        cargarDatos()
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