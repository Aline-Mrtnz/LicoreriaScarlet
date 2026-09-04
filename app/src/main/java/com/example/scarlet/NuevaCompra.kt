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

        productos = productosRepository.obtenerTodosLosProductos()
        proveedores = proveedoresRepository.listar(filtroEstado = ProveedoresRepository.ESTADO_ACTIVO)
        configurarSpinnerProveedor()

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
        val total = filas.sumOf { fila ->
            val cantidad = fila.edtCantidad.text.toString().toIntOrNull() ?: 0
            val precio = fila.edtPrecio.text.toString().toDoubleOrNull() ?: 0.0
            cantidad * precio
        }
        txtTotalCompra.text = ComprasAdapter.formatearBs(total)
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

        val proveedor = proveedores[spinnerProveedor.selectedItemPosition - 1]
        val cuentaId = if (Session.estaLogueado) Session.idCuenta else 1

        val id = comprasRepository.crearCompra(
            idProveedor = proveedor.idProveedor,
            observacion = edtObservacion.text.toString().trim().ifBlank { null },
            cuentaIdCuenta = cuentaId,
            detalles = detalles
        )

        if (id > 0) {
            Toast.makeText(this, "Compra registrada como pendiente", Toast.LENGTH_SHORT).show()
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