package com.example.scarlet

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.scarlet.adapter.CartAdapter
import com.example.scarlet.cart.CartManager
import com.example.scarlet.data.repository.CuentaRepository
import com.example.scarlet.data.repository.PagosRepository
import com.example.scarlet.data.repository.PersonaRepository
import com.example.scarlet.data.repository.VentasRepository
import com.example.scarlet.util.FechaUtils
import com.example.scarlet.util.Session
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.text.NumberFormat
import java.util.Locale

class Shopping : AppCompatActivity() {

    private lateinit var recyclerViewCarrito: RecyclerView
    private lateinit var adapter: CartAdapter

    private lateinit var ventasRepository: VentasRepository
    private lateinit var pagosRepository: PagosRepository
    private lateinit var personaRepository: PersonaRepository
    private lateinit var cuentaRepository: CuentaRepository

    // El carrito vive en CartManager (compartido con Inicio/Productos), así
    // que aquí solo mantenemos una referencia a esa misma lista.
    private val carrito get() = CartManager.obtenerItems()

    // TextViews del resumen
    private lateinit var txtSubtotal: TextView
    private lateinit var txtImpuestos: TextView
    private lateinit var txtTotal: TextView
    private lateinit var tvSubtotalLabel: TextView
    private lateinit var tvCarritoVacio: TextView
    private lateinit var btnCheckout: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_shopping)

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

        ventasRepository = VentasRepository(this)
        pagosRepository = PagosRepository(this)
        personaRepository = PersonaRepository(this)
        cuentaRepository = CuentaRepository(this)

        // Inicializar TextViews del resumen
        txtSubtotal = findViewById(R.id.txtSubtotal)
        txtImpuestos = findViewById(R.id.txtImpuestos)
        txtTotal = findViewById(R.id.txtTotal)
        tvSubtotalLabel = findViewById(R.id.tvSubtotalLabel)
        tvCarritoVacio = findViewById(R.id.tvCarritoVacio)
        btnCheckout = findViewById(R.id.btnFinalizarCompra)

        // Configurar RecyclerView del carrito
        configurarRecyclerView()

        // Configurar botón de volver
        setupBackButton()

        // Configurar botón "Añadir más productos"
        setupAddMoreButton()

        // Configurar botón "Finalizar Compra"
        setupCheckoutButton()

        // Configurar navegación inferior
        setupBottomNavigation()

        // Actualizar resumen inicial
        actualizarVista()
    }

    override fun onResume() {
        super.onResume()
        // Si el usuario agregó productos en otra pantalla y vuelve aquí,
        // refrescamos la lista y el resumen.
        adapter.notifyDataSetChanged()
        actualizarVista()
    }

    private fun configurarRecyclerView() {
        recyclerViewCarrito = findViewById(R.id.recyclerViewCarrito)
        recyclerViewCarrito.layoutManager = LinearLayoutManager(this)
        recyclerViewCarrito.isNestedScrollingEnabled = false

        adapter = CartAdapter(carrito) {
            // Se llama cada vez que el usuario cambia una cantidad o elimina un producto
            adapter.notifyDataSetChanged()
            actualizarVista()
        }

        recyclerViewCarrito.adapter = adapter
    }

    private fun actualizarVista() {
        val estaVacio = carrito.isEmpty()
        tvCarritoVacio.visibility = if (estaVacio) android.view.View.VISIBLE else android.view.View.GONE
        recyclerViewCarrito.visibility = if (estaVacio) android.view.View.GONE else android.view.View.VISIBLE
        btnCheckout.isEnabled = !estaVacio
        btnCheckout.alpha = if (estaVacio) 0.5f else 1f
        updateSummary()
    }

    private fun updateSummary() {
        // Calcular subtotal a partir de los items del carrito
        val subtotal = carrito.sumOf { it.subtotal }

        // Calcular impuestos (16%)
        val impuestos = subtotal * 0.16

        // Calcular total
        val total = subtotal + impuestos

        // Actualizar TextViews
        txtSubtotal.text = formatPrice(subtotal)
        txtImpuestos.text = formatPrice(impuestos)
        txtTotal.text = formatPrice(total)

        val totalItems = carrito.sumOf { it.cantidad }
        tvSubtotalLabel.text = "Subtotal ($totalItems items)"
    }

    private fun formatPrice(amount: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale.US)
        return format.format(amount)
    }

    private fun setupBackButton() {
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun setupAddMoreButton() {
        val btnAddMore = findViewById<Button>(R.id.btnAddMore)
        btnAddMore.setOnClickListener {
            val intent = Intent(this, Productos::class.java)
            startActivity(intent)
        }
    }

    private fun setupCheckoutButton() {
        btnCheckout.setOnClickListener {
            if (carrito.isEmpty()) {
                Toast.makeText(this, "Tu carrito está vacío", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            mostrarDialogoMetodoPago()
        }
    }

    private fun mostrarDialogoMetodoPago() {
        val metodosPago = pagosRepository.listar()
        if (metodosPago.isEmpty()) {
            Toast.makeText(this, "No hay métodos de pago configurados", Toast.LENGTH_SHORT).show()
            return
        }

        val nombres = metodosPago.map { it.tipo_pago }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Selecciona un método de pago")
            .setItems(nombres) { _, indice ->
                val pagoSeleccionado = metodosPago[indice]
                registrarVenta(pagoSeleccionado.id_pago)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun registrarVenta(idPago: Int) {
        val idCliente = personaRepository.obtenerOCrearClienteMostrador()
        val idCuenta = if (Session.estaLogueado) {
            Session.idCuenta
        } else {
            cuentaRepository.obtenerUsuarioActual()?.idCuenta ?: 1
        }

        val resultado = ventasRepository.registrarVentaCompleta(
            fecha = FechaUtils.ahora(),
            idCliente = idCliente,
            idPago = idPago,
            idCuenta = idCuenta,
            items = carrito
        )

        when (resultado) {
            is VentasRepository.ResultadoVenta.Exito -> {
                CartManager.limpiar()
                adapter.notifyDataSetChanged()
                actualizarVista()
                Toast.makeText(this, "¡Venta registrada correctamente!", Toast.LENGTH_LONG).show()
                val intent = Intent(this, Ventas::class.java)
                startActivity(intent)
                finish()
            }
            is VentasRepository.ResultadoVenta.SinStock -> {
                Toast.makeText(
                    this,
                    "Stock insuficiente para ${resultado.nombreProducto}",
                    Toast.LENGTH_LONG
                ).show()
            }
            is VentasRepository.ResultadoVenta.Error -> {
                Toast.makeText(this, "Error al registrar la venta: ${resultado.mensaje}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_productos

        bottomNav.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_productos -> {
                    // Ya estamos en Shopping
                    true
                }
                R.id.nav_ventas -> {
                    val intent = Intent(this, Ventas::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_reportes -> {
                    val intent = Intent(this, Reportes::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
    }
}
