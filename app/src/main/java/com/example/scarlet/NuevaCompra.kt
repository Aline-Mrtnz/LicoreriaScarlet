package com.example.scarlet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.scarlet.adapter.ComprasAdapter
import com.example.scarlet.data.model.DetalleCompra
import com.example.scarlet.data.model.Producto
import com.example.scarlet.data.model.Proveedor
import com.example.scarlet.data.repository.ComprasRepository
import com.example.scarlet.data.repository.ProductosRepository
import com.example.scarlet.data.repository.ProveedoresRepository
import com.example.scarlet.util.Session

class NuevaCompra : AppCompatActivity() {

    private lateinit var comprasRepository: ComprasRepository
    private lateinit var productosRepository: ProductosRepository
    private lateinit var proveedoresRepository: ProveedoresRepository

    private lateinit var spinnerProveedor: Spinner
    private lateinit var edtObservacion: EditText
    private lateinit var llProductos: LinearLayout
    private lateinit var txtTotalCompra: TextView

    // --- Forma de pago al proveedor (nuevo) ---
    private lateinit var txtCondicionProveedor: TextView
    private lateinit var chipPagoPendiente: TextView
    private lateinit var chipPagoContado: TextView
    private lateinit var chipPagoAnticipo: TextView
    private lateinit var txtNotaPagoPendiente: TextView
    private lateinit var layoutDatosPago: LinearLayout
    private lateinit var spinnerMetodoPagoInicial: Spinner
    private lateinit var layoutMontoAnticipo: LinearLayout
    private lateinit var edtMontoAnticipo: EditText
    private lateinit var txtResumenPagoInicial: TextView

    /** "PENDIENTE" (todo a crédito), "CONTADO" (paga el total ahora) o "ANTICIPO" (paga una parte ahora). */
    private var tipoPagoSeleccionado = "PENDIENTE"

    private var proveedores: List<Proveedor> = emptyList()
    private var productos: List<Producto> = emptyList()
    private val filas = mutableListOf<FilaProducto>()

    private inner class FilaProducto(
        val view: View,
        val spinner: Spinner,
        val edtCantidad: EditText,
        val edtPrecio: EditText,
        val txtSubtotal: TextView,
        val btnQuitar: View
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nueva_compra)

        if (!Session.esAdmin) {
            Toast.makeText(this, "Acceso solo para administradores", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        comprasRepository = ComprasRepository(this)
        productosRepository = ProductosRepository(this)
        proveedoresRepository = ProveedoresRepository(this)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<TextView>(R.id.btnCancelar).setOnClickListener { finish() }

        spinnerProveedor = findViewById(R.id.spinnerProveedor)
        edtObservacion = findViewById(R.id.edtObservacion)
        llProductos = findViewById(R.id.llProductosCompra)
        txtTotalCompra = findViewById(R.id.txtTotalCompra)

        txtCondicionProveedor = findViewById(R.id.txtCondicionProveedor)
        chipPagoPendiente = findViewById(R.id.chipPagoPendiente)
        chipPagoContado = findViewById(R.id.chipPagoContado)
        chipPagoAnticipo = findViewById(R.id.chipPagoAnticipo)
        txtNotaPagoPendiente = findViewById(R.id.txtNotaPagoPendiente)
        layoutDatosPago = findViewById(R.id.layoutDatosPago)
        spinnerMetodoPagoInicial = findViewById(R.id.spinnerMetodoPagoInicial)
        layoutMontoAnticipo = findViewById(R.id.layoutMontoAnticipo)
        edtMontoAnticipo = findViewById(R.id.edtMontoAnticipo)
        txtResumenPagoInicial = findViewById(R.id.txtResumenPagoInicial)

        spinnerMetodoPagoInicial.adapter = crearAdapterSpinner(listOf("Efectivo", "Transferencia", "QR"))

        productos = productosRepository.obtenerTodosLosProductos()
        proveedores = proveedoresRepository.listar(filtroEstado = ProveedoresRepository.ESTADO_ACTIVO)
        configurarSpinnerProveedor()
        configurarFormaDePago()

        findViewById<TextView>(R.id.btnAgregarProducto).setOnClickListener { agregarFila() }
        findViewById<TextView>(R.id.btnRegistrarCompra).setOnClickListener { registrarCompra() }

        val idProductoSugerido = intent.getIntExtra(EXTRA_ID_PRODUCTO, -1)
        agregarFila(idProductoSugerido)
    }

    private fun configurarSpinnerProveedor() {
        if (proveedores.isEmpty()) {
            spinnerProveedor.adapter = crearAdapterSpinner(listOf("Sin proveedores activos registrados"))
            spinnerProveedor.isEnabled = false
            return
        }
        val nombres = listOf("Seleccionar proveedor...") + proveedores.map { it.razonSocial }
        spinnerProveedor.adapter = crearAdapterSpinner(nombres)

        spinnerProveedor.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: android.widget.AdapterView<*>?, v: View?, pos: Int, id: Long) {
                if (pos > 0) {
                    val proveedor = proveedores[pos - 1]
                    val condicion = proveedor.condicionPago.takeIf { it.isNotBlank() } ?: "No especificada"
                    txtCondicionProveedor.text = "Este proveedor suele trabajar con: $condicion"
                } else {
                    txtCondicionProveedor.text = "Selecciona un proveedor para ver su condición de pago habitual."
                }
            }
            override fun onNothingSelected(p: android.widget.AdapterView<*>?) {}
        }
    }

    /**
     * Antes esta pantalla no preguntaba NADA sobre la forma de pago: la
     * compra siempre quedaba 100% pendiente, sin decir si correspondía
     * pagar al contado, a crédito/cuotas, o dejar un anticipo. Ahora hay 3
     * chips que cambian qué se pide y qué se guarda:
     *
     *  - "Todo a crédito": no se pide nada más; la orden queda 100% pendiente
     *    (se podrán registrar abonos/cuotas después, desde el detalle).
     *  - "Al contado": se pide método de pago y se paga el TOTAL de una vez
     *    al momento de registrar la compra (saldo pendiente = 0).
     *  - "Con anticipo": se pide método de pago + un monto de adelanto
     *    (menor al total), mostrando en vivo cuánto quedará pendiente.
     */
    private fun configurarFormaDePago() {
        chipPagoPendiente.setOnClickListener { seleccionarTipoPago("PENDIENTE") }
        chipPagoContado.setOnClickListener { seleccionarTipoPago("CONTADO") }
        chipPagoAnticipo.setOnClickListener { seleccionarTipoPago("ANTICIPO") }

        edtMontoAnticipo.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) { actualizarResumenPago() }
        })

        seleccionarTipoPago("PENDIENTE")
    }

    private fun seleccionarTipoPago(tipo: String) {
        tipoPagoSeleccionado = tipo

        chipPagoPendiente.setBackgroundResource(if (tipo == "PENDIENTE") R.drawable.bg_filter_selected else R.drawable.bg_filter_unselected)
        chipPagoContado.setBackgroundResource(if (tipo == "CONTADO") R.drawable.bg_filter_selected else R.drawable.bg_filter_unselected)
        chipPagoAnticipo.setBackgroundResource(if (tipo == "ANTICIPO") R.drawable.bg_filter_selected else R.drawable.bg_filter_unselected)

        txtNotaPagoPendiente.visibility = if (tipo == "PENDIENTE") View.VISIBLE else View.GONE
        layoutDatosPago.visibility = if (tipo == "PENDIENTE") View.GONE else View.VISIBLE
        layoutMontoAnticipo.visibility = if (tipo == "ANTICIPO") View.VISIBLE else View.GONE

        actualizarResumenPago()
    }

    private fun totalActual(): Double = filas.sumOf { fila ->
        val cantidad = fila.edtCantidad.text.toString().toIntOrNull() ?: 0
        val precio = fila.edtPrecio.text.toString().toDoubleOrNull() ?: 0.0
        cantidad * precio
    }

    /** Refresca "Vas a pagar Bs X ahora. Saldo pendiente: Bs Y." según el tipo de pago elegido y el total actual. */
    private fun actualizarResumenPago() {
        val total = totalActual()
        val montoAPagar = when (tipoPagoSeleccionado) {
            "CONTADO" -> total
            "ANTICIPO" -> edtMontoAnticipo.text.toString().toDoubleOrNull() ?: 0.0
            else -> 0.0
        }
        val saldo = (total - montoAPagar).coerceAtLeast(0.0)
        txtResumenPagoInicial.text =
            "Vas a pagar ${ComprasAdapter.formatearBs(montoAPagar)} ahora. Saldo pendiente: ${ComprasAdapter.formatearBs(saldo)}."
    }

    private fun agregarFila(idProductoSugerido: Int = -1) {
        if (productos.isEmpty()) {
            Toast.makeText(this, "No hay productos registrados en el catálogo", Toast.LENGTH_SHORT).show()
            return
        }

        val filaView = LayoutInflater.from(this)
            .inflate(R.layout.item_producto_compra_row, llProductos, false)

        val spinner = filaView.findViewById<Spinner>(R.id.spinnerProductoFila)
        val edtCantidad = filaView.findViewById<EditText>(R.id.edtCantidadFila)
        val edtPrecio = filaView.findViewById<EditText>(R.id.edtPrecioFila)
        val txtSubtotal = filaView.findViewById<TextView>(R.id.txtSubtotalFila)
        val btnQuitar = filaView.findViewById<View>(R.id.btnQuitarFila)

        val nombres = listOf("Seleccionar producto...") + productos.map { it.nombreProducto }
        spinner.adapter = crearAdapterSpinner(nombres)

        val watcher = object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                recalcularFila(edtCantidad, edtPrecio, txtSubtotal)
                recalcularTotal()
            }
        }
        edtCantidad.addTextChangedListener(watcher)
        edtPrecio.addTextChangedListener(watcher)

        spinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: android.widget.AdapterView<*>?, v: View?, pos: Int, id: Long) {
                if (pos > 0) {
                    val producto = productos[pos - 1]
                    edtPrecio.setText(String.format("%.2f", producto.precioCompra ?: 0.0))
                    if (edtCantidad.text.isBlank()) {
                        edtCantidad.setText(producto.stockMinimo.coerceAtLeast(1).toString())
                    }
                }
                recalcularFila(edtCantidad, edtPrecio, txtSubtotal)
                recalcularTotal()
            }
            override fun onNothingSelected(p: android.widget.AdapterView<*>?) {}
        }

        btnQuitar.setOnClickListener {
            llProductos.removeView(filaView)
            filas.removeAll { it.view == filaView }
            actualizarVisibilidadQuitar()
            recalcularTotal()
        }

        llProductos.addView(filaView)
        filas.add(FilaProducto(filaView, spinner, edtCantidad, edtPrecio, txtSubtotal, btnQuitar))
        actualizarVisibilidadQuitar()

        if (idProductoSugerido > 0) {
            val index = productos.indexOfFirst { it.idProducto == idProductoSugerido }
            if (index >= 0) spinner.setSelection(index + 1)
        }
    }

    private fun actualizarVisibilidadQuitar() {
        filas.forEach { it.btnQuitar.visibility = if (filas.size > 1) View.VISIBLE else View.GONE }
    }

    private fun recalcularFila(edtCantidad: EditText, edtPrecio: EditText, txtSubtotal: TextView) {
        val cantidad = edtCantidad.text.toString().toIntOrNull() ?: 0
        val precio = edtPrecio.text.toString().toDoubleOrNull() ?: 0.0
        txtSubtotal.text = ComprasAdapter.formatearBs(cantidad * precio)
    }

    private fun recalcularTotal() {
        val total = totalActual()
        txtTotalCompra.text = ComprasAdapter.formatearBs(total)
        actualizarResumenPago()
    }

    private fun registrarCompra() {
        if (proveedores.isEmpty() || spinnerProveedor.selectedItemPosition == 0) {
            Toast.makeText(this, "Selecciona un proveedor", Toast.LENGTH_SHORT).show()
            return
        }

        val detalles = mutableListOf<DetalleCompra>()
        for (fila in filas) {
            val posProducto = fila.spinner.selectedItemPosition
            if (posProducto == 0) {
                Toast.makeText(this, "Selecciona un producto en cada fila", Toast.LENGTH_SHORT).show()
                return
            }
            val cantidad = fila.edtCantidad.text.toString().toIntOrNull() ?: 0
            val precio = fila.edtPrecio.text.toString().toDoubleOrNull() ?: 0.0
            if (cantidad <= 0 || precio <= 0) {
                Toast.makeText(this, "Revisa la cantidad y el precio de cada producto", Toast.LENGTH_SHORT).show()
                return
            }
            val producto = productos[posProducto - 1]
            detalles.add(
                DetalleCompra(
                    cantidad = cantidad,
                    precioUnitario = precio,
                    subtotal = cantidad * precio,
                    idProducto = producto.idProducto
                )
            )
        }

        if (detalles.isEmpty()) {
            Toast.makeText(this, "Agrega al menos un producto", Toast.LENGTH_SHORT).show()
            return
        }

        val total = detalles.sumOf { it.subtotal }

        // Validar la forma de pago elegida (nuevo). Antes no se pedía nada
        // aquí y la orden quedaba siempre 100% pendiente sin más detalle.
        var montoPagoInicial: Double? = null
        var metodoPagoInicial: String? = null
        when (tipoPagoSeleccionado) {
            "CONTADO" -> {
                montoPagoInicial = total
                metodoPagoInicial = spinnerMetodoPagoInicial.selectedItem.toString()
            }
            "ANTICIPO" -> {
                val monto = edtMontoAnticipo.text.toString().toDoubleOrNull()
                if (monto == null || monto <= 0) {
                    Toast.makeText(this, "Ingresa el monto del anticipo", Toast.LENGTH_SHORT).show()
                    return
                }
                if (monto > total + 0.009) {
                    Toast.makeText(this, "El anticipo no puede ser mayor al total de la compra", Toast.LENGTH_SHORT).show()
                    return
                }
                montoPagoInicial = monto
                metodoPagoInicial = spinnerMetodoPagoInicial.selectedItem.toString()
            }
            // "PENDIENTE": no se registra ningún pago, la orden queda a crédito.
        }

        val proveedor = proveedores[spinnerProveedor.selectedItemPosition - 1]
        val cuentaId = if (Session.estaLogueado) Session.idCuenta else 1
        val registradoPor = if (Session.estaLogueado) Session.nombreCompleto else null

        val id = comprasRepository.crearCompraConPagoInicial(
            idProveedor = proveedor.idProveedor,
            observacion = edtObservacion.text.toString().trim().ifBlank { null },
            cuentaIdCuenta = cuentaId,
            detalles = detalles,
            montoPagoInicial = montoPagoInicial,
            metodoPagoInicial = metodoPagoInicial,
            registradoPor = registradoPor
        )

        if (id > 0) {
            val mensaje = when (tipoPagoSeleccionado) {
                "CONTADO" -> "Compra registrada y pagada al contado"
                "ANTICIPO" -> "Compra registrada con anticipo de ${ComprasAdapter.formatearBs(montoPagoInicial ?: 0.0)}"
                else -> "Compra registrada como pendiente (a crédito)"
            }
            Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
            finish()
        } else {
            Toast.makeText(this, "No se pudo registrar la compra", Toast.LENGTH_SHORT).show()
        }
    }

    private fun crearAdapterSpinner(items: List<String>): ArrayAdapter<String> {
        return ArrayAdapter(this, R.layout.spinner_item_selected, items).apply {
            setDropDownViewResource(R.layout.spinner_item_dropdown)
        }
    }

    companion object {
        const val EXTRA_ID_PRODUCTO = "extra_id_producto"
    }
}