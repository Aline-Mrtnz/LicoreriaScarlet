package com.example.scarlet

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
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.text.NumberFormat
import java.util.Locale

class Shopping : AppCompatActivity() {

    // Variables para cantidades
    private var cantidadMacallan = 1
    private var cantidadHennessy = 2

    // Precios base
    private val precioMacallanBase = 450.00
    private val precioHennessyBase = 280.00

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

        // Configurar botón de volver
        setupBackButton()

        // Configurar controles de cantidad
        setupQuantityControls()

        // Configurar botón "Añadir más productos"
        setupAddMoreButton()

        // Configurar botón "Finalizar Compra"
        setupCheckoutButton()

        // Configurar navegación inferior
        setupBottomNavigation()

        // Actualizar resumen inicial
        updateSummary()
    }

    private fun setupBackButton() {
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun setupQuantityControls() {
        // Macallan
        setupProductQuantity(
            btnRestar = R.id.btnRestarMacallan,
            btnSumar = R.id.btnSumarMacallan,
            txtCantidad = R.id.cantidadMacallan,
            txtPrecio = R.id.precioMacallan,
            cantidad = { cantidadMacallan },
            setCantidad = { cantidadMacallan = it },
            precioBase = precioMacallanBase
        )

        // Hennessy
        setupProductQuantity(
            btnRestar = R.id.btnRestarHennessy,
            btnSumar = R.id.btnSumarHennessy,
            txtCantidad = R.id.cantidadHennessy,
            txtPrecio = R.id.precioHennessy,
            cantidad = { cantidadHennessy },
            setCantidad = { cantidadHennessy = it },
            precioBase = precioHennessyBase
        )
    }

    private fun setupProductQuantity(
        btnRestar: Int,
        btnSumar: Int,
        txtCantidad: Int,
        txtPrecio: Int,
        cantidad: () -> Int,
        setCantidad: (Int) -> Unit,
        precioBase: Double
    ) {
        val tvCantidad = findViewById<TextView>(txtCantidad)
        val tvPrecio = findViewById<TextView>(txtPrecio)

        findViewById<Button>(btnRestar).setOnClickListener {
            if (cantidad() > 1) {
                setCantidad(cantidad() - 1)
                tvCantidad.text = cantidad().toString()
                val total = precioBase * cantidad()
                tvPrecio.text = formatPrice(total)
                updateSummary()
            } else {
                Toast.makeText(this, "La cantidad mínima es 1", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(btnSumar).setOnClickListener {
            setCantidad(cantidad() + 1)
            tvCantidad.text = cantidad().toString()
            val total = precioBase * cantidad()
            tvPrecio.text = formatPrice(total)
            updateSummary()
        }
    }

    private fun updateSummary() {
        // Calcular subtotal
        val subtotal = (precioMacallanBase * cantidadMacallan) + (precioHennessyBase * cantidadHennessy)

        // Calcular impuestos (16%)
        val impuestos = subtotal * 0.16

        // Calcular total
        val total = subtotal + impuestos

        // Actualizar TextViews
        txtSubtotal.text = formatPrice(subtotal)
        txtImpuestos.text = formatPrice(impuestos)
        txtTotal.text = formatPrice(total)

        // Actualizar texto del subtotal con cantidad de items
        val totalItems = cantidadMacallan + cantidadHennessy
        findViewById<TextView>(R.id.txtSubtotal).text =
            formatPrice(subtotal)
        // Nota: El texto "Subtotal (3 items)" está fijo en el XML
        // Podrías actualizarlo dinámicamente si quieres
    }

    private fun formatPrice(amount: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale.US)
        return format.format(amount)
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
            intent.putExtra("items", cantidadMacallan + cantidadHennessy)
            startActivity(intent)
        }
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_productos

        bottomNav.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    Toast.makeText(this, "Home", Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(this, "Profile", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }
    }
}