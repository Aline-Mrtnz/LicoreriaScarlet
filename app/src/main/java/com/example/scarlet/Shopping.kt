package com.example.scarlet

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.scarlet.adapter.CartAdapter
import com.example.scarlet.data.model.CartItem
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.text.NumberFormat
import java.util.Locale

class Shopping : AppCompatActivity() {

    private lateinit var recyclerViewCarrito: RecyclerView
    private lateinit var adapter: CartAdapter

    // Carrito de ejemplo. En una integración real esto vendría de un
    // repositorio/CartManager compartido con la pantalla de Productos.
    private val carrito = mutableListOf(
        CartItem(
            idProducto = 1,
            nombre = "Macallan 18 Years",
            categoria = "SINGLE MALT SCOTCH",
            precioUnitario = 450.00,
            imagenResId = R.drawable.macallan18,
            cantidad = 1
        ),
        CartItem(
            idProducto = 2,
            nombre = "Hennessy X.O",
            categoria = "COGNAC",
            precioUnitario = 280.00,
            imagenResId = R.drawable.hennessyxo,
            cantidad = 2
        )
    )

    // TextViews del resumen
    private lateinit var txtSubtotal: TextView
    private lateinit var txtImpuestos: TextView
    private lateinit var txtTotal: TextView

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

        // Inicializar TextViews del resumen
        txtSubtotal = findViewById(R.id.txtSubtotal)
        txtImpuestos = findViewById(R.id.txtImpuestos)
        txtTotal = findViewById(R.id.txtTotal)

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
        updateSummary()
    }

    private fun configurarRecyclerView() {
        recyclerViewCarrito = findViewById(R.id.recyclerViewCarrito)
        recyclerViewCarrito.layoutManager = LinearLayoutManager(this)
        recyclerViewCarrito.isNestedScrollingEnabled = false

        adapter = CartAdapter(carrito) {
            // Se llama cada vez que el usuario cambia una cantidad
            updateSummary()
        }

        recyclerViewCarrito.adapter = adapter
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

        // Nota: El texto "Subtotal (N items)" está fijo en el XML.
        // val totalItems = carrito.sumOf { it.cantidad }
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
        val btnCheckout = findViewById<Button>(R.id.btnFinalizarCompra)
        btnCheckout.setOnClickListener {
            // Ir a la ventana de Sales
            val intent = Intent(this, Ventas::class.java)
            // Enviar datos del carrito
            intent.putExtra("subtotal", txtSubtotal.text.toString())
            intent.putExtra("impuestos", txtImpuestos.text.toString())
            intent.putExtra("total", txtTotal.text.toString())
            intent.putExtra("items", carrito.sumOf { it.cantidad })
            startActivity(intent)
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
