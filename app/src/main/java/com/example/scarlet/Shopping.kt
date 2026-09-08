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
import com.example.scarlet.util.ComprobanteUtils
import com.example.scarlet.util.FechaUtils
import com.example.scarlet.util.ImpuestosUtils
import com.example.scarlet.util.Session
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.activity.result.contract.ActivityResultContracts
import java.text.NumberFormat
import java.util.Locale
import android.view.ViewGroup
import android.view.View
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.EditText


class Shopping : AppCompatActivity() {

    // Guarda el id del método de pago QR mientras esperamos la confirmación
    // de la pantalla QR (que se abre en una Activity aparte).
    private var idPagoPendienteQR: Int? = null

    private val qrLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { resultado ->
        val idPago = idPagoPendienteQR
        idPagoPendienteQR = null
        if (resultado.resultCode == RESULT_OK && idPago != null) {
            registrarVenta(idPago)
        }
        // Si el cajero canceló/volvió atrás sin confirmar, no se registra
        // nada y el carrito sigue intacto para reintentar.
    }

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

        val header = findViewById<android.view.View>(R.id.headerLayout)
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.main)
        ) { _, insets ->

            val systemBars =
                insets.getInsets(WindowInsetsCompat.Type.systemBars())

            // HEADER
            val headerParams =
                header.layoutParams as ViewGroup.MarginLayoutParams

            headerParams.height =
                (82 * resources.displayMetrics.density).toInt() + systemBars.top

            header.layoutParams = headerParams

            header.setPadding(
                header.paddingLeft,
                systemBars.top,
                header.paddingRight,
                header.paddingBottom
            )// BOTTOM NAVIGATION
            val bottomParams =
                bottomNavigation.layoutParams as ViewGroup.MarginLayoutParams

            bottomParams.height =
                (80 * resources.displayMetrics.density).toInt() + systemBars.bottom

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

        // Los precios de catálogo ya incluyen IVA (13%) + IT (3%): el total
        // a cobrar es exactamente la suma de los precios del carrito, sin
        // sumar impuestos aparte. El desglose de IVA/IT es solo informativo
        // (se extrae del precio final, no se agrega encima).
        val desglose = ImpuestosUtils.desdeTotalConImpuestos(subtotal)

        // Actualizar TextViews
        txtSubtotal.text = formatPrice(desglose.subtotal)
        txtImpuestos.text = formatPrice(desglose.impuestos)
        txtTotal.text = formatPrice(desglose.total)

        val totalItems = carrito.sumOf { it.cantidad }
        tvSubtotalLabel.text = "Subtotal ($totalItems items)"
    }

    private fun formatPrice(amount: Double): String {
        return "Bs " + String.format(Locale("es", "BO"), "%,.2f", amount)
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

                when {
                    pagoSeleccionado.tipo_pago.equals("QR", ignoreCase = true) -> {
                        // El pago con QR necesita que el cajero confirme en la
                        // pantalla QR (donde se muestra el código de cobro y el
                        // monto real a pagar, que no es editable) antes de
                        // registrar la venta.
                        idPagoPendienteQR = pagoSeleccionado.id_pago
                        val subtotal = carrito.sumOf { it.subtotal }
                        val total = subtotal
                        val intent = Intent(this, QR::class.java).apply {
                            putExtra("total", formatPrice(total))
                        }
                        qrLauncher.launch(intent)
                    }
                    pagoSeleccionado.tipo_pago.equals("Efectivo", ignoreCase = true) -> {
                        // En efectivo se mantiene el flujo actual: se registra
                        // directo (registrarVenta ya exige que la caja esté
                        // abierta antes de continuar).
                        registrarVenta(pagoSeleccionado.id_pago)
                    }
                    else -> {
                        // Cualquier otro método (Tarjeta de Crédito, Tarjeta de
                        // Débito, Transferencia, etc.): el monto a depositar
                        // debe ser exactamente el total de la compra, sin
                        // permitir registrar un monto distinto.
                        mostrarConfirmacionPagoElectronico(pagoSeleccionado.id_pago, pagoSeleccionado.tipo_pago)
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /**
     * Confirmación previa para métodos de pago electrónicos (todo lo que no
     * sea Efectivo ni QR): el campo viene prellenado con el total exacto de
     * la compra y se valida que lo que se confirme coincida con ese total
     * (no se permite un monto mayor ni menor) antes de registrar la venta.
     */
    private fun mostrarConfirmacionPagoElectronico(idPago: Int, nombreMetodo: String) {
        val total = carrito.sumOf { it.subtotal }
        val vista = LayoutInflater.from(this).inflate(R.layout.dialog_confirmar_pago_electronico, null)

        vista.findViewById<TextView>(R.id.txtTituloPagoElectronico).text = "Confirmar pago con $nombreMetodo"

        val edtMonto = vista.findViewById<EditText>(R.id.edtMontoPagoElectronico)
        //edtMonto.setText(String.format(Locale("es", "BO"), "%.2f", total))
        edtMonto.setText(String.format(Locale.US, "%.2f", total))

        val dialog = AlertDialog.Builder(this)
            .setView(vista)
            .setCancelable(true)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        vista.findViewById<TextView>(R.id.btnCancelarPagoElectronico).setOnClickListener {
            dialog.dismiss()
        }

        vista.findViewById<TextView>(R.id.btnConfirmarPagoElectronico).setOnClickListener {
            //val monto = edtMonto.text.toString().toDoubleOrNull()
            val monto = edtMonto.text.toString().replace(",", ".").toDoubleOrNull()
            if (monto == null) {
                Toast.makeText(this, "Ingresa un monto válido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // Tolerancia mínima para evitar falsos negativos por redondeo de
            // centavos (comparación de Double).
            if (Math.abs(monto - total) > 0.009) {
                Toast.makeText(
                    this,
                    "Debes depositar exactamente ${formatPrice(total)}, ni más ni menos",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }
            dialog.dismiss()
            registrarVenta(idPago)
        }

        dialog.show()
    }

    private fun registrarVenta(idPago: Int) {
        val idCliente = personaRepository.obtenerOCrearClienteMostrador()
        val idCuenta = if (Session.estaLogueado) {
            Session.idCuenta
        } else {
            cuentaRepository.obtenerUsuarioActual()?.idCuenta ?: 1
        }

        if (com.example.scarlet.data.repository.TurnoCajaRepository(this).turnoAbierto(idCuenta) == null) {
            Toast.makeText(this, "Debes abrir la caja antes de registrar ventas", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, CajaActivity::class.java))
            return
        }

        val fechaVenta = FechaUtils.ahora()

        // Guardamos una copia del carrito y de los datos de la venta ANTES de
        // registrarla, porque registrarVentaCompleta() no nos devuelve el
        // detalle y CartManager.limpiar() vacía la lista original.
        val itemsRecibo = carrito.map { Triple(it.nombre, it.cantidad, it.subtotal) }
        val subtotalRecibo = carrito.sumOf { it.subtotal }
        // Los precios ya incluyen IVA+IT, así que el total cobrado es el
        // mismo subtotalRecibo; el desglose solo separa cuánto de ese monto
        // corresponde a impuestos (no se suma nada extra).
        val desglose = ImpuestosUtils.desdeTotalConImpuestos(subtotalRecibo)
        val nombreCliente = personaRepository.obtenerPorId(idCliente)
            ?.let { "${it.nombres} ${it.apellidos}" } ?: "Cliente Mostrador"
        val nombreMetodoPago = pagosRepository.obtenerPorId(idPago)?.tipo_pago ?: "-"
        val nombreCajero = if (Session.estaLogueado) Session.nombreCompleto else "Admin Sistema"

        val resultado = ventasRepository.registrarVentaCompleta(
            fecha = fechaVenta,
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
                mostrarReciboVenta(
                    idVenta = resultado.idVenta.toInt(),
                    fecha = fechaVenta,
                    cliente = nombreCliente,
                    cajero = nombreCajero,
                    metodoPago = nombreMetodoPago,
                    items = itemsRecibo,
                    subtotal = desglose.subtotal,
                    iva = desglose.iva,
                    impuestoIt = desglose.it,
                    total = desglose.total
                )
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

    /**
     * Muestra el recibo de la venta recién registrada (requisito: recibo tras
     * cada venta). Desde aquí el cajero puede imprimir/guardar el comprobante
     * como PDF y luego continuar hacia el historial de Ventas.
     */
    private fun mostrarReciboVenta(
        idVenta: Int,
        fecha: String,
        cliente: String,
        cajero: String,
        metodoPago: String,
        items: List<Triple<String, Int, Double>>,
        subtotal: Double,
        iva: Double,
        impuestoIt: Double,
        total: Double
    ) {
        val vista = LayoutInflater.from(this).inflate(R.layout.dialog_recibo_venta, null)

        vista.findViewById<TextView>(R.id.txtNumeroReciboVenta).text = "Venta #$idVenta · Confirmada"
        vista.findViewById<TextView>(R.id.txtFechaReciboVenta).text = "Fecha: $fecha"
        vista.findViewById<TextView>(R.id.txtClienteReciboVenta).text = "Cliente: $cliente"
        vista.findViewById<TextView>(R.id.txtCajeroReciboVenta).text = "Atendido por: $cajero"
        vista.findViewById<TextView>(R.id.txtMetodoPagoReciboVenta).text = "Método de pago: $metodoPago"
        vista.findViewById<TextView>(R.id.txtSubtotalReciboVenta).text = formatPrice(subtotal)
        vista.findViewById<TextView>(R.id.txtIvaReciboVenta).text = formatPrice(iva)
        vista.findViewById<TextView>(R.id.txtItReciboVenta).text = formatPrice(impuestoIt)
        vista.findViewById<TextView>(R.id.txtTotalReciboVenta).text = formatPrice(total)

        val llLineas = vista.findViewById<LinearLayout>(R.id.llLineasRecibo)
        items.forEach { (nombre, cantidad, subtotalLinea) ->
            val fila = LayoutInflater.from(this).inflate(R.layout.item_linea_recibo, llLineas, false)
            val precioUnit = if (cantidad > 0) subtotalLinea / cantidad else subtotalLinea
            fila.findViewById<TextView>(R.id.txtNombreLineaRecibo).text = nombre
            fila.findViewById<TextView>(R.id.txtCantidadLineaRecibo).text = "x$cantidad"
            fila.findViewById<TextView>(R.id.txtPrecioUnitLineaRecibo).text = "${formatPrice(precioUnit)} c/u"
            fila.findViewById<TextView>(R.id.txtSubtotalLineaRecibo).text = formatPrice(subtotalLinea)
            llLineas.addView(fila)
        }

        val dialog = AlertDialog.Builder(this)
            .setView(vista)
            .setCancelable(false)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        vista.findViewById<TextView>(R.id.btnImprimirReciboVenta).setOnClickListener {
            ComprobanteUtils.imprimirVenta(
                context = this,
                idVenta = idVenta,
                fecha = fecha,
                cajero = cajero,
                cliente = cliente,
                metodoPago = metodoPago,
                items = items,
                subtotal = subtotal,
                iva = iva,
                impuestoIt = impuestoIt,
                total = total
            )
        }

        vista.findViewById<TextView>(R.id.btnContinuarReciboVenta).setOnClickListener {
            dialog.dismiss()
            startActivity(Intent(this, Ventas::class.java))
            finish()
        }

        dialog.show()
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