// app/src/main/java/com/example/scarlet/Proveedores.kt

package com.example.scarlet

import android.app.AlertDialog
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.scarlet.adapter.ProveedoresAdapter
import com.example.scarlet.util.Session
import com.example.scarlet.data.model.Producto
import com.example.scarlet.data.model.Proveedor
import com.example.scarlet.data.repository.ProductosRepository
import com.example.scarlet.data.repository.ProveedoresRepository

class Proveedores : AppCompatActivity() {

    private lateinit var proveedoresRepository: ProveedoresRepository
    private lateinit var productosRepository: ProductosRepository

    private lateinit var recyclerViewProveedores: RecyclerView
    private lateinit var adapter: ProveedoresAdapter

    private lateinit var edtBuscar: EditText
    private lateinit var filtroTodos: TextView
    private lateinit var filtroActivos: TextView
    private lateinit var filtroInactivos: TextView
    private lateinit var tvTotalProveedores: TextView
    private lateinit var tvSinResultados: TextView

    // Filtro de estado actualmente seleccionado: null = todos
    private var filtroEstadoActual: String? = null
    private var textoBusquedaActual: String = ""

    // Opciones fijas de condición de pago para el spinner del formulario
    private val condicionesPago = listOf(
        "Pago Contado", "Crédito 15d", "Crédito 30d", "Crédito 45d", "Crédito 60d"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_proveedores)

        if (!Session.esAdmin) {
            Toast.makeText(this, "Acceso solo para administradores", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        proveedoresRepository = ProveedoresRepository(this)
        productosRepository = ProductosRepository(this)

        vincularVistas()
        configurarEdgeToEdge()
        configurarRecyclerView()
        configurarBuscador()
        configurarFiltros()
        configurarBotones()

        cargarProveedores()
    }

    override fun onResume() {
        super.onResume()
        // Por si se volvió de otra pantalla que pudo modificar productos/proveedores.
        cargarProveedores()
    }

    // =============================================
    // VISTAS
    // =============================================

    private fun vincularVistas() {
        recyclerViewProveedores = findViewById(R.id.recyclerViewProveedores)
        edtBuscar = findViewById(R.id.edtBuscarProveedor)
        filtroTodos = findViewById(R.id.filtroTodos)
        filtroActivos = findViewById(R.id.filtroActivos)
        filtroInactivos = findViewById(R.id.filtroInactivos)
        tvTotalProveedores = findViewById(R.id.tvTotalProveedores)
        tvSinResultados = findViewById(R.id.tvSinResultados)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }

    private fun configurarEdgeToEdge() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun configurarRecyclerView() {
        adapter = ProveedoresAdapter(
            proveedores = emptyList(),
            onProductosClick = { proveedor -> mostrarDialogoProductos(proveedor) },
            onEditarClick = { proveedor -> mostrarDialogoProveedor(proveedor) },
            onEliminarClick = { proveedor -> confirmarEliminarProveedor(proveedor) },
            onEstadoCambiado = { proveedor, activo -> cambiarEstadoProveedor(proveedor, activo) }
        )
        recyclerViewProveedores.layoutManager = LinearLayoutManager(this)
        recyclerViewProveedores.adapter = adapter
    }

    private fun configurarBuscador() {
        edtBuscar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                textoBusquedaActual = s?.toString()?.trim() ?: ""
                cargarProveedores()
            }
        })
    }

    private fun configurarFiltros() {
        filtroTodos.setOnClickListener {
            filtroEstadoActual = null
            marcarFiltroSeleccionado(filtroTodos)
            cargarProveedores()
        }
        filtroActivos.setOnClickListener {
            filtroEstadoActual = ProveedoresRepository.ESTADO_ACTIVO
            marcarFiltroSeleccionado(filtroActivos)
            cargarProveedores()
        }
        filtroInactivos.setOnClickListener {
            filtroEstadoActual = ProveedoresRepository.ESTADO_INACTIVO
            marcarFiltroSeleccionado(filtroInactivos)
            cargarProveedores()
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
        findViewById<LinearLayout>(R.id.btnAnadirProveedor).setOnClickListener {
            mostrarDialogoProveedor(null)
        }
    }

    // =============================================
    // CARGAR / FILTRAR LISTA
    // =============================================

    private fun cargarProveedores() {
        val lista = proveedoresRepository.listar(
            filtroEstado = filtroEstadoActual,
            busqueda = textoBusquedaActual.ifBlank { null }
        )
        adapter.actualizarLista(lista)

        val (activos, inactivos) = proveedoresRepository.contarPorEstado()
        val total = activos + inactivos
        filtroTodos.text = "Todos ($total)"
        filtroActivos.text = "Activos ($activos)"
        filtroInactivos.text = "Inactivos ($inactivos)"
        tvTotalProveedores.text = "${lista.size} en total"

        tvSinResultados.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
        recyclerViewProveedores.visibility = if (lista.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun cambiarEstadoProveedor(proveedor: Proveedor, activo: Boolean) {
        val nuevoEstado = if (activo) ProveedoresRepository.ESTADO_ACTIVO else ProveedoresRepository.ESTADO_INACTIVO
        proveedoresRepository.cambiarEstado(proveedor.idProveedor, nuevoEstado)
        cargarProveedores()
    }

    // =============================================
    // ELIMINAR PROVEEDOR
    // =============================================

    private fun confirmarEliminarProveedor(proveedor: Proveedor) {
        AlertDialog.Builder(this)
            .setTitle("Scarlet Licorería")
            .setMessage(
                "Esto BORRA al proveedor \"${proveedor.razonSocial}\" de forma permanente e irreversible.\n\n¿Confirmas que quieras eliminarlo?"
            )
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Aceptar") { _, _ ->
                proveedoresRepository.eliminar(proveedor.idProveedor)
                Toast.makeText(this, "Proveedor eliminado", Toast.LENGTH_SHORT).show()
                cargarProveedores()
            }
            .show()
    }

    // =============================================
    // DIÁLOGO BASE (reutilizado para ambos formularios)
    // =============================================

    private fun crearDialogoBase(layoutRes: Int): Pair<Dialog, View> {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        val vista = LayoutInflater.from(this).inflate(layoutRes, null)
        dialog.setContentView(vista)

        dialog.window?.let { window ->
            window.setBackgroundDrawable(ColorDrawable(Color.parseColor("#CC000000")))
            window.setLayout(
                (resources.displayMetrics.widthPixels * 0.92).toInt(),
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            window.setGravity(Gravity.CENTER)
        }

        return Pair(dialog, vista)
    }

    private fun crearAdapterSpinner(items: List<String>): ArrayAdapter<String> {
        return ArrayAdapter(this, R.layout.spinner_item_selected, items).apply {
            setDropDownViewResource(R.layout.spinner_item_dropdown)
        }
    }

    // =============================================
    // DIÁLOGO: NUEVO / EDITAR PROVEEDOR
    // =============================================

    private fun mostrarDialogoProveedor(proveedorExistente: Proveedor?) {
        val (dialog, vista) = crearDialogoBase(R.layout.dialog_proveedor_form)

        val tvTitulo = vista.findViewById<TextView>(R.id.tvTituloDialogo)
        val edtRazonSocial = vista.findViewById<EditText>(R.id.edtRazonSocial)
        val edtRfcNit = vista.findViewById<EditText>(R.id.edtRfcNit)
        val spinnerCondicionPago = vista.findViewById<Spinner>(R.id.spinnerCondicionPago)
        val edtMarcasAsociadas = vista.findViewById<EditText>(R.id.edtMarcasAsociadas)
        val edtContactoEjecutivo = vista.findViewById<EditText>(R.id.edtContactoEjecutivo)
        val edtTelefonoContacto = vista.findViewById<EditText>(R.id.edtTelefonoContacto)
        val btnCerrar = vista.findViewById<ImageView>(R.id.btnCerrarDialogo)
        val btnCancelar = vista.findViewById<TextView>(R.id.btnCancelarProveedor)
        val btnGuardar = vista.findViewById<TextView>(R.id.btnGuardarProveedor)

        spinnerCondicionPago.adapter = crearAdapterSpinner(condicionesPago)

        val esEdicion = proveedorExistente != null
        tvTitulo.text = if (esEdicion) "Editar Proveedor" else "Nuevo Proveedor"
        btnGuardar.text = if (esEdicion) "Guardar Cambios" else "Guardar Proveedor"

        if (proveedorExistente != null) {
            edtRazonSocial.setText(proveedorExistente.razonSocial)
            edtRfcNit.setText(proveedorExistente.rfcNit)
            edtMarcasAsociadas.setText(proveedorExistente.marcasAsociadas)
            edtContactoEjecutivo.setText(proveedorExistente.contactoEjecutivo)
            edtTelefonoContacto.setText(proveedorExistente.telefonoContacto)

            val indice = condicionesPago.indexOf(proveedorExistente.condicionPago)
            if (indice >= 0) spinnerCondicionPago.setSelection(indice)
        }

        btnCerrar.setOnClickListener { dialog.dismiss() }
        btnCancelar.setOnClickListener { dialog.dismiss() }

        btnGuardar.setOnClickListener {
            val razonSocial = edtRazonSocial.text.toString().trim()
            val rfcNit = edtRfcNit.text.toString().trim()
            val condicionPago = spinnerCondicionPago.selectedItem?.toString() ?: condicionesPago.first()
            val marcas = edtMarcasAsociadas.text.toString().trim()
            val contacto = edtContactoEjecutivo.text.toString().trim()
            val telefono = edtTelefonoContacto.text.toString().trim()

            if (razonSocial.isEmpty()) {
                edtRazonSocial.error = "Ingresa la razón social"
                edtRazonSocial.requestFocus()
                return@setOnClickListener
            }

            if (rfcNit.isEmpty()) {
                edtRfcNit.error = "Ingresa el RFC / NIT"
                edtRfcNit.requestFocus()
                return@setOnClickListener
            }

            if (esEdicion) {
                val actualizado = proveedorExistente!!.copy(
                    razonSocial = razonSocial,
                    rfcNit = rfcNit,
                    condicionPago = condicionPago,
                    marcasAsociadas = marcas,
                    contactoEjecutivo = contacto,
                    telefonoContacto = telefono
                )
                proveedoresRepository.editar(actualizado)
                Toast.makeText(this, "Proveedor actualizado", Toast.LENGTH_SHORT).show()
            } else {
                val nuevo = Proveedor(
                    razonSocial = razonSocial,
                    rfcNit = rfcNit,
                    condicionPago = condicionPago,
                    marcasAsociadas = marcas,
                    contactoEjecutivo = contacto,
                    telefonoContacto = telefono,
                    estado = ProveedoresRepository.ESTADO_ACTIVO
                )
                proveedoresRepository.crear(nuevo)
                Toast.makeText(this, "Proveedor registrado", Toast.LENGTH_SHORT).show()
            }

            cargarProveedores()
            dialog.dismiss()
        }

        dialog.show()
    }

    // =============================================
    // DIÁLOGO: PRODUCTOS DEL PROVEEDOR
    // =============================================

    private fun mostrarDialogoProductos(proveedor: Proveedor) {
        val (dialog, vista) = crearDialogoBase(R.layout.dialog_productos_proveedor)

        val tvTitulo = vista.findViewById<TextView>(R.id.tvTituloProductosDialogo)
        val btnCerrarDialogo = vista.findViewById<ImageView>(R.id.btnCerrarProductosDialogo)
        val spinnerProducto = vista.findViewById<Spinner>(R.id.spinnerProducto)
        val edtPrecioPactado = vista.findViewById<EditText>(R.id.edtPrecioPactado)
        val btnAgregar = vista.findViewById<TextView>(R.id.btnAgregarProducto)
        val llFilasProductos = vista.findViewById<LinearLayout>(R.id.llFilasProductos)
        val tvSinProductos = vista.findViewById<TextView>(R.id.tvSinProductosVinculados)
        val tvContador = vista.findViewById<TextView>(R.id.tvContadorProductosVinculados)
        val btnCerrar = vista.findViewById<TextView>(R.id.btnCerrarProductos)

        tvTitulo.text = "Productos de ${proveedor.razonSocial}"

        val productosDisponibles: List<Producto> = productosRepository
            .obtenerTodosLosProductos()
            .sortedBy { it.nombreProducto }

        val nombresProductos = listOf("Selecciona un producto...") + productosDisponibles.map { it.nombreProducto }
        spinnerProducto.adapter = crearAdapterSpinner(nombresProductos)

        fun refrescarFilas() {
            llFilasProductos.removeAllViews()
            val vinculados = proveedoresRepository.listarProductosDeProveedor(proveedor.idProveedor)

            tvSinProductos.visibility = if (vinculados.isEmpty()) View.VISIBLE else View.GONE
            tvContador.text = if (vinculados.size == 1) "1 producto vinculado" else "${vinculados.size} productos vinculados"

            vinculados.forEach { vinculo ->
                val filaVista = LayoutInflater.from(this)
                    .inflate(R.layout.item_producto_proveedor, llFilasProductos, false)

                filaVista.findViewById<TextView>(R.id.tvNombreProductoFila).text = vinculo.nombreProducto
                filaVista.findViewById<TextView>(R.id.tvUnidadFila).text = vinculo.unidad
                filaVista.findViewById<TextView>(R.id.tvPrecioFila).text =
                    "Bs %.2f".format(vinculo.precioPactado)

                filaVista.findViewById<TextView>(R.id.tvQuitarFila).setOnClickListener {
                    proveedoresRepository.quitarProductoDeProveedor(vinculo.idProveedorProducto)
                    refrescarFilas()
                }

                llFilasProductos.addView(filaVista)
            }
        }

        refrescarFilas()

        btnAgregar.setOnClickListener {
            val posicionSeleccionada = spinnerProducto.selectedItemPosition
            if (posicionSeleccionada <= 0) {
                Toast.makeText(this, "Selecciona un producto", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val precioTexto = edtPrecioPactado.text.toString().trim()
            val precio = precioTexto.toDoubleOrNull()
            if (precio == null || precio <= 0.0) {
                edtPrecioPactado.error = "Ingresa un precio válido"
                edtPrecioPactado.requestFocus()
                return@setOnClickListener
            }

            val productoSeleccionado = productosDisponibles[posicionSeleccionada - 1]
            val resultado = proveedoresRepository.agregarProductoAProveedor(
                idProveedor = proveedor.idProveedor,
                idProducto = productoSeleccionado.idProducto,
                precioPactado = precio
            )

            if (resultado == -1L) {
                Toast.makeText(this, "Ese producto ya está vinculado a este proveedor", Toast.LENGTH_SHORT).show()
            } else {
                edtPrecioPactado.setText("")
                spinnerProducto.setSelection(0)
                refrescarFilas()
            }
        }

        btnCerrarDialogo.setOnClickListener { dialog.dismiss() }
        btnCerrar.setOnClickListener { dialog.dismiss() }

        dialog.setOnDismissListener {
            cargarProveedores()
        }

        dialog.show()
    }
}