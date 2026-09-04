// app/src/main/java/com/example/scarlet/Inventario.kt

package com.example.scarlet

import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.scarlet.adapter.InventarioAdapter
import com.example.scarlet.adapter.MovimientoAdapter
import com.example.scarlet.data.model.Marcas
import com.example.scarlet.data.model.Producto
import com.example.scarlet.data.model.Proveedor
import com.example.scarlet.data.repository.CategoriasRepository
import com.example.scarlet.data.repository.InventarioRepository
import com.example.scarlet.data.repository.MarcasRepository
import com.example.scarlet.data.repository.ProductosRepository
import com.example.scarlet.data.repository.ProveedoresRepository
import com.example.scarlet.util.ImagenUtils
import com.example.scarlet.util.Session
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

class Inventario : AppCompatActivity() {

    private lateinit var productosRepository: ProductosRepository
    private lateinit var inventarioRepository: InventarioRepository
    private lateinit var proveedoresRepository: ProveedoresRepository
    private lateinit var categoriasRepository: CategoriasRepository
    private lateinit var marcasRepository: MarcasRepository

    private lateinit var recyclerInventario: RecyclerView
    private lateinit var adapter: InventarioAdapter

    private lateinit var edtBuscar: EditText
    private lateinit var spinnerCategoria: Spinner
    private lateinit var switchSoloStockBajo: androidx.appcompat.widget.SwitchCompat
    private lateinit var txtValorTotalStock: TextView
    private lateinit var chipTodos: TextView
    private lateinit var chipStockBajo: TextView
    private lateinit var chipAlertas: TextView
    private lateinit var txtTotalChips: TextView
    private lateinit var tabExistencias: TextView
    private lateinit var tabMatriz: TextView
    private lateinit var tabAlertas: TextView
    private lateinit var txtInventarioVacio: TextView

    private var listaCompleta: List<Producto> = emptyList()
    private var nombresCategorias: List<String> = emptyList()

    // "Todos" | "Stock Bajo" | "Alertas"
    private var chipSeleccionado: String = "Todos"
    private var categoriaSeleccionada: String? = null // null = todas

    // --- Estado temporal para el picker de imagen de la Ficha Técnica ---
    private var imgFichaPreviewActivo: ImageView? = null
    private var txtFichaImagenNombreActivo: TextView? = null
    private var rutaImagenSeleccionadaFicha: String? = null

    private val seleccionarImagenLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) manejarImagenSeleccionada(uri)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inventario)

        if (!Session.esAdmin) {
            Toast.makeText(this, "Acceso solo para administradores", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        productosRepository = ProductosRepository(this)
        inventarioRepository = InventarioRepository(this)
        proveedoresRepository = ProveedoresRepository(this)
        categoriasRepository = CategoriasRepository(this)
        marcasRepository = MarcasRepository(this)

        vincularVistas()
        configurarRecycler()
        configurarListeners()
        configurarSpinnerCategoria()

        cargarDatos()

        // Si se abrió desde el menú "Reabastecimiento" o desde la alerta de stock bajo
        if (intent.getBooleanExtra(EXTRA_ABRIR_ALERTAS, false)) {
            tabAlertas.performClick()
        }
    }

    override fun onResume() {
        super.onResume()
        // Por si se creó/editó un producto, proveedor o categoría en otra pantalla.
        cargarDatos()
    }

    // =========================================================
    // VINCULACIÓN DE VISTAS
    // =========================================================
    private fun vincularVistas() {
        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        recyclerInventario = findViewById(R.id.recyclerInventario)
        edtBuscar = findViewById(R.id.edtBuscarInventario)
        spinnerCategoria = findViewById(R.id.spinnerCategoriaInventario)
        switchSoloStockBajo = findViewById(R.id.switchSoloStockBajo)
        txtValorTotalStock = findViewById(R.id.txtValorTotalStock)
        chipTodos = findViewById(R.id.chipTodos)
        chipStockBajo = findViewById(R.id.chipStockBajo)
        chipAlertas = findViewById(R.id.chipAlertas)
        txtTotalChips = findViewById(R.id.txtTotalChips)
        tabExistencias = findViewById(R.id.tabExistencias)
        tabMatriz = findViewById(R.id.tabMatriz)
        tabAlertas = findViewById(R.id.tabAlertas)
        txtInventarioVacio = findViewById(R.id.txtInventarioVacio)
    }

    private fun configurarRecycler() {
        adapter = InventarioAdapter(
            productos = emptyList(),
            onClickCard = { producto -> mostrarDialogoHistorial(producto) },
            onReabastecer = { producto -> mostrarDialogoReabastecer(producto) },
            onEditar = { producto -> mostrarDialogoFicha(producto) },
            onEliminar = { producto -> mostrarDialogoConfirmarEliminar(producto) },
            onToggleActivo = { producto, activo -> cambiarEstadoProducto(producto, activo) }
        )
        recyclerInventario.layoutManager = LinearLayoutManager(this)
        recyclerInventario.adapter = adapter
    }

    private fun configurarListeners() {
        findViewById<LinearLayout>(R.id.btnReabastecerStock).setOnClickListener {
            mostrarDialogoReabastecer(null)
        }

        edtBuscar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { aplicarFiltros() }
        })

        switchSoloStockBajo.setOnCheckedChangeListener { _, isChecked ->
            chipSeleccionado = if (isChecked) "Alertas" else "Todos"
            actualizarEstadoChips()
            aplicarFiltros()
        }

        chipTodos.setOnClickListener {
            chipSeleccionado = "Todos"
            switchSoloStockBajo.isChecked = false
            actualizarEstadoChips()
            aplicarFiltros()
        }
        chipStockBajo.setOnClickListener {
            chipSeleccionado = "Stock Bajo"
            switchSoloStockBajo.isChecked = false
            actualizarEstadoChips()
            aplicarFiltros()
        }
        chipAlertas.setOnClickListener {
            chipSeleccionado = "Alertas"
            switchSoloStockBajo.isChecked = true
            actualizarEstadoChips()
            aplicarFiltros()
        }

        tabExistencias.setOnClickListener {
            seleccionarTab(tabExistencias)
            chipSeleccionado = "Todos"
            switchSoloStockBajo.isChecked = false
            actualizarEstadoChips()
            aplicarFiltros()
        }
        tabMatriz.setOnClickListener {
            seleccionarTab(tabMatriz)
            // La "Matriz de Stock" es la misma lista con la tarjeta de resumen
            // como protagonista; no aplica un filtro adicional.
        }
        tabAlertas.setOnClickListener {
            seleccionarTab(tabAlertas)
            chipSeleccionado = "Alertas"
            switchSoloStockBajo.isChecked = true
            actualizarEstadoChips()
            aplicarFiltros()
        }
    }

    private fun seleccionarTab(seleccionado: TextView) {
        listOf(tabExistencias, tabMatriz, tabAlertas).forEach { tab ->
            if (tab == seleccionado) {
                tab.setBackgroundResource(R.drawable.bg_tab_selected)
                tab.setTextColor(0xFFFFFFFF.toInt())
            } else {
                tab.setBackgroundResource(R.drawable.bg_tab_unselected)
                tab.setTextColor(0xFF999999.toInt())
            }
        }
    }

    private fun actualizarEstadoChips() {
        val seleccionado = R.drawable.bg_filter_selected
        val noSeleccionado = R.drawable.bg_filter_unselected

        chipTodos.setBackgroundResource(if (chipSeleccionado == "Todos") seleccionado else noSeleccionado)
        chipTodos.setTextColor(if (chipSeleccionado == "Todos") 0xFFFFFFFF.toInt() else 0xFF999999.toInt())

        chipStockBajo.setBackgroundResource(if (chipSeleccionado == "Stock Bajo") seleccionado else noSeleccionado)
        chipStockBajo.setTextColor(if (chipSeleccionado == "Stock Bajo") 0xFFFFFFFF.toInt() else 0xFF999999.toInt())

        chipAlertas.setBackgroundResource(if (chipSeleccionado == "Alertas") seleccionado else noSeleccionado)
        chipAlertas.setTextColor(if (chipSeleccionado == "Alertas") 0xFFFFFFFF.toInt() else 0xFF999999.toInt())
    }

    private fun configurarSpinnerCategoria() {
        val categorias = categoriasRepository.listarActivas()
        nombresCategorias = categorias.map { it.nombre_categoria }
        val opciones = listOf("Todas las categorías") + nombresCategorias
        spinnerCategoria.adapter = crearAdapterSpinner(opciones)
        spinnerCategoria.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                categoriaSeleccionada = if (position == 0) null else nombresCategorias[position - 1]
                aplicarFiltros()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
    }

    private fun crearAdapterSpinner(items: List<String>): ArrayAdapter<String> {
        return ArrayAdapter(this, R.layout.spinner_item_selected, items).apply {
            setDropDownViewResource(R.layout.spinner_item_dropdown)
        }
    }

    // =========================================================
    // CARGA Y FILTRADO DE DATOS
    // =========================================================
    private fun cargarDatos() {
        listaCompleta = inventarioRepository.obtenerProductosInventario()
        txtValorTotalStock.text = "Bs ${formatearMonto(inventarioRepository.obtenerValorTotalStock())}"
        aplicarFiltros()
    }

    private fun aplicarFiltros() {
        val texto = edtBuscar.text?.toString()?.trim()?.lowercase(Locale.getDefault()) ?: ""

        // Filtro base: categoría + texto de búsqueda.
        var lista = listaCompleta
        if (categoriaSeleccionada != null) {
            lista = lista.filter { it.nombreCategoria == categoriaSeleccionada }
        }
        if (texto.isNotEmpty()) {
            lista = lista.filter { producto ->
                producto.nombreProducto.lowercase(Locale.getDefault()).contains(texto) ||
                        (producto.nombreCategoria ?: "").lowercase(Locale.getDefault()).contains(texto) ||
                        (producto.nombreMarca ?: "").lowercase(Locale.getDefault()).contains(texto)
            }
        }

        val totalTodos = lista.size
        val totalStockBajo = lista.count { it.stock in 1..it.stockMinimo }
        val totalAlertas = lista.count { it.stock <= it.stockMinimo }

        chipTodos.text = "Todos ($totalTodos)"
        chipStockBajo.text = "Stock Bajo ($totalStockBajo)"
        chipAlertas.text = "Alertas ($totalAlertas)"
        tabAlertas.text = "Alertas • $totalAlertas"

        val listaFinal = when (chipSeleccionado) {
            "Stock Bajo" -> lista.filter { it.stock in 1..it.stockMinimo }
            "Alertas" -> lista.filter { it.stock <= it.stockMinimo }
            else -> lista
        }

        txtTotalChips.text = "${listaFinal.size} en total"
        adapter.actualizar(listaFinal)

        val vacio = listaFinal.isEmpty()
        txtInventarioVacio.visibility = if (vacio) View.VISIBLE else View.GONE
        recyclerInventario.visibility = if (vacio) View.GONE else View.VISIBLE
    }

    private fun cambiarEstadoProducto(producto: Producto, activo: Boolean) {
        val exito = inventarioRepository.cambiarEstadoProducto(producto.idProducto, activo)
        if (exito) {
            val mensaje = if (activo)
                "\"${producto.nombreProducto}\" ahora está activo"
            else
                "\"${producto.nombreProducto}\" ahora está inactivo"
            Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "No se pudo actualizar el estado del producto", Toast.LENGTH_SHORT).show()
        }
        cargarDatos()
    }

    // =========================================================
    // DIÁLOGO: REABASTECER INVENTARIO
    // =========================================================
    private fun mostrarDialogoReabastecer(productoPreseleccionado: Producto?) {
        if (listaCompleta.isEmpty()) {
            Toast.makeText(this, "No hay productos registrados todavía", Toast.LENGTH_SHORT).show()
            return
        }

        val vista = LayoutInflater.from(this).inflate(R.layout.dialog_reabastecer_inventario, null)
        val dialog = AlertDialog.Builder(this).setView(vista).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val spinnerProveedor = vista.findViewById<Spinner>(R.id.spinnerProveedor)
        val spinnerProducto = vista.findViewById<Spinner>(R.id.spinnerProductoReabastecer)
        val edtCantidad = vista.findViewById<EditText>(R.id.edtCantidadReabastecer)
        val edtPrecio = vista.findViewById<EditText>(R.id.edtPrecioUnitarioReabastecer)
        val txtSubtotal = vista.findViewById<TextView>(R.id.txtSubtotalReabastecer)
        val txtTotal = vista.findViewById<TextView>(R.id.txtTotalReabastecer)
        val edtNotas = vista.findViewById<EditText>(R.id.edtNotasReabastecer)

        val proveedores = proveedoresRepository.listar(ProveedoresRepository.ESTADO_ACTIVO)
        val nombresProveedores = listOf("Sin proveedor") + proveedores.map { it.razonSocial }
        spinnerProveedor.adapter = crearAdapterSpinner(nombresProveedores)

        val nombresProductos = listaCompleta.map { it.nombreProducto }
        spinnerProducto.adapter = crearAdapterSpinner(nombresProductos)
        val indiceProductoPreseleccionado = productoPreseleccionado?.let { listaCompleta.indexOf(it) } ?: -1
        if (indiceProductoPreseleccionado >= 0) spinnerProducto.setSelection(indiceProductoPreseleccionado)

        fun proveedorSeleccionado(): Proveedor? {
            val pos = spinnerProveedor.selectedItemPosition
            return if (pos <= 0) null else proveedores[pos - 1]
        }

        fun productoSeleccionado(): Producto? {
            val pos = spinnerProducto.selectedItemPosition
            return if (pos in listaCompleta.indices) listaCompleta[pos] else null
        }

        fun recalcularTotales() {
            val cantidad = edtCantidad.text.toString().toIntOrNull() ?: 0
            val precio = edtPrecio.text.toString().toDoubleOrNull() ?: 0.0
            val subtotal = cantidad * precio
            txtSubtotal.text = "Bs ${formatearMonto(subtotal)}"
            txtTotal.text = "Bs ${formatearMonto(subtotal)}"
        }

        fun autocompletarPrecio() {
            val proveedor = proveedorSeleccionado()
            val producto = productoSeleccionado()
            if (proveedor != null && producto != null && edtPrecio.text.isNullOrBlank()) {
                val pactado = inventarioRepository.obtenerPrecioPactado(proveedor.idProveedor, producto.idProducto)
                if (pactado != null) {
                    edtPrecio.setText(String.format(Locale.getDefault(), "%.2f", pactado))
                    recalcularTotales()
                }
            }
        }

        spinnerProveedor.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                autocompletarPrecio()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
        spinnerProducto.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                autocompletarPrecio()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        val watcherTotales = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { recalcularTotales() }
        }
        edtCantidad.addTextChangedListener(watcherTotales)
        edtPrecio.addTextChangedListener(watcherTotales)

        vista.findViewById<ImageView>(R.id.btnCerrarReabastecer).setOnClickListener { dialog.dismiss() }
        vista.findViewById<TextView>(R.id.btnCancelarReabastecer).setOnClickListener { dialog.dismiss() }

        vista.findViewById<TextView>(R.id.btnRegistrarCompra).setOnClickListener {
            val producto = productoSeleccionado()
            val cantidad = edtCantidad.text.toString().toIntOrNull() ?: 0
            val precio = edtPrecio.text.toString().toDoubleOrNull()

            if (producto == null) {
                Toast.makeText(this, "Selecciona un producto", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (cantidad <= 0) {
                Toast.makeText(this, "Ingresa una cantidad válida", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val proveedor = proveedorSeleccionado()
            val notas = edtNotas.text?.toString()?.trim()

            val exito = inventarioRepository.registrarEntrada(
                idProducto = producto.idProducto,
                cantidad = cantidad,
                idProveedor = proveedor?.idProveedor,
                precioUnitarioPactado = precio,
                notas = if (notas.isNullOrBlank()) null else notas,
                usuario = if (Session.usuario.isNotBlank()) Session.usuario else "admin"
            )

            if (exito) {
                Toast.makeText(this, "Compra registrada: +$cantidad unidades de \"${producto.nombreProducto}\"", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                cargarDatos()
            } else {
                Toast.makeText(this, "No se pudo registrar la compra", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    // =========================================================
    // DIÁLOGO: FICHA TÉCNICA (EDITAR PRODUCTO)
    // =========================================================
    private fun mostrarDialogoFicha(producto: Producto) {
        val vista = LayoutInflater.from(this).inflate(R.layout.dialog_ficha_producto, null)
        val dialog = AlertDialog.Builder(this).setView(vista).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val edtNombre = vista.findViewById<EditText>(R.id.edtFichaNombre)
        val spinnerMarca = vista.findViewById<Spinner>(R.id.spinnerFichaMarca)
        val edtDescripcion = vista.findViewById<EditText>(R.id.edtFichaDescripcion)
        val edtVolumen = vista.findViewById<EditText>(R.id.edtFichaVolumen)
        val edtGrado = vista.findViewById<EditText>(R.id.edtFichaGrado)
        val edtCosto = vista.findViewById<EditText>(R.id.edtFichaCosto)
        val edtPrecioVenta = vista.findViewById<EditText>(R.id.edtFichaPrecioVenta)
        val edtStockMinimo = vista.findViewById<EditText>(R.id.edtFichaStockMinimo)
        val frameImagen = vista.findViewById<LinearLayout>(R.id.frameFichaImagen)
        val imgPreview = vista.findViewById<ImageView>(R.id.imgFichaPreview)
        val txtImagenNombre = vista.findViewById<TextView>(R.id.txtFichaImagenNombre)

        edtNombre.setText(producto.nombreProducto)
        edtDescripcion.setText(producto.descripcion ?: "")
        edtVolumen.setText(producto.volumenMl?.toString() ?: "")
        edtGrado.setText(producto.abv?.toString() ?: "")
        edtCosto.setText(producto.precioCompra?.toString() ?: "")
        edtPrecioVenta.setText(producto.precioVenta.toString())
        edtStockMinimo.setText(producto.stockMinimo.toString())
        ImagenUtils.cargarEnImageView(this, imgPreview, producto.imagen)

        val marcas: List<Marcas> = marcasRepository.listar()
        val nombresMarcas = listOf("Sin marca") + marcas.map { it.nombre_marca }
        spinnerMarca.adapter = crearAdapterSpinner(nombresMarcas)
        val indiceMarca = marcas.indexOfFirst { it.id_marca == producto.marcasIdMarca }
        spinnerMarca.setSelection(if (indiceMarca >= 0) indiceMarca + 1 else 0)

        // Preparamos el estado para que el resultado del selector de imagen
        // (que llega de forma asíncrona) sepa a qué diálogo/vista actualizar.
        rutaImagenSeleccionadaFicha = null
        imgFichaPreviewActivo = imgPreview
        txtFichaImagenNombreActivo = txtImagenNombre

        frameImagen.setOnClickListener {
            seleccionarImagenLauncher.launch("image/*")
        }

        vista.findViewById<ImageView>(R.id.btnCerrarFicha).setOnClickListener { dialog.dismiss() }
        vista.findViewById<TextView>(R.id.btnCancelarFicha).setOnClickListener { dialog.dismiss() }

        vista.findViewById<TextView>(R.id.btnGuardarFicha).setOnClickListener {
            val nombre = edtNombre.text.toString().trim()
            val precioVenta = edtPrecioVenta.text.toString().toDoubleOrNull()

            if (nombre.isEmpty()) {
                Toast.makeText(this, "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (precioVenta == null || precioVenta <= 0) {
                Toast.makeText(this, "Ingresa un precio de venta válido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val posMarca = spinnerMarca.selectedItemPosition
            val idMarca = if (posMarca <= 0) null else marcas[posMarca - 1].id_marca

            val productoActualizado = producto.copy(
                nombreProducto = nombre,
                descripcion = edtDescripcion.text.toString().trim().ifEmpty { null },
                imagen = rutaImagenSeleccionadaFicha ?: producto.imagen,
                precioVenta = precioVenta,
                precioCompra = edtCosto.text.toString().toDoubleOrNull(),
                stockMinimo = edtStockMinimo.text.toString().toIntOrNull() ?: producto.stockMinimo,
                marcasIdMarca = idMarca,
                volumenMl = edtVolumen.text.toString().toIntOrNull(),
                abv = edtGrado.text.toString().toDoubleOrNull()
            )

            val exito = productosRepository.editarProducto(productoActualizado)
            if (exito) {
                Toast.makeText(this, "Ficha técnica actualizada", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                cargarDatos()
            } else {
                Toast.makeText(this, "No se pudo guardar la ficha técnica", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun manejarImagenSeleccionada(uri: Uri) {
        val imgPreview = imgFichaPreviewActivo ?: return
        val txtNombre = txtFichaImagenNombreActivo
        try {
            val bitmap: Bitmap = contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input)
            } ?: return

            val carpeta = File(filesDir, "productos_img").apply { mkdirs() }
            val archivo = File(carpeta, "producto_${System.currentTimeMillis()}.jpg")
            FileOutputStream(archivo).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }

            rutaImagenSeleccionadaFicha = archivo.absolutePath
            imgPreview.setImageBitmap(bitmap)
            txtNombre?.text = "Imagen seleccionada"
        } catch (e: Exception) {
            Toast.makeText(this, "No se pudo cargar la imagen seleccionada", Toast.LENGTH_SHORT).show()
        }
    }

    // =========================================================
    // DIÁLOGO: HISTORIAL DE MOVIMIENTOS
    // =========================================================
    private fun mostrarDialogoHistorial(producto: Producto) {
        val vista = LayoutInflater.from(this).inflate(R.layout.dialog_historial_movimientos, null)
        val dialog = AlertDialog.Builder(this).setView(vista).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        vista.findViewById<TextView>(R.id.txtHistorialProducto).text = producto.nombreProducto

        val spinnerTipo = vista.findViewById<Spinner>(R.id.spinnerTipoMovimiento)
        val recycler = vista.findViewById<RecyclerView>(R.id.recyclerMovimientos)
        val txtVacio = vista.findViewById<TextView>(R.id.txtHistorialVacio)

        val tipos = listOf("Todos los tipos", "ENTRADA", "SALIDA")
        spinnerTipo.adapter = crearAdapterSpinner(tipos)

        val movimientoAdapter = MovimientoAdapter(emptyList())
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = movimientoAdapter

        fun cargarMovimientos() {
            val tipoSeleccionado = tipos[spinnerTipo.selectedItemPosition]
            val movimientos = inventarioRepository.obtenerMovimientos(producto.idProducto, tipoSeleccionado)
            movimientoAdapter.actualizar(movimientos)
            val vacio = movimientos.isEmpty()
            txtVacio.visibility = if (vacio) View.VISIBLE else View.GONE
            recycler.visibility = if (vacio) View.GONE else View.VISIBLE
        }

        spinnerTipo.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                cargarMovimientos()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        cargarMovimientos()

        vista.findViewById<ImageView>(R.id.btnCerrarHistorial).setOnClickListener { dialog.dismiss() }
        vista.findViewById<TextView>(R.id.btnCerrarHistorialFooter).setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    // =========================================================
    // DIÁLOGO: CONFIRMAR ELIMINACIÓN (DESACTIVACIÓN) DE PRODUCTO
    // =========================================================
    private fun mostrarDialogoConfirmarEliminar(producto: Producto) {
        val vista = LayoutInflater.from(this).inflate(R.layout.dialog_confirmar_eliminar, null)
        val dialog = AlertDialog.Builder(this).setView(vista).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        vista.findViewById<TextView>(R.id.txtMensajeEliminar).text =
            "Esto desactivará \"${producto.nombreProducto}\" y dejará de aparecer en el catálogo de ventas. " +
                    "Podrás reactivarlo cuando quieras desde este mismo listado. ¿Confirmas que quieres continuar?"

        vista.findViewById<TextView>(R.id.btnCancelarEliminar).setOnClickListener { dialog.dismiss() }
        vista.findViewById<TextView>(R.id.btnAceptarEliminar).setOnClickListener {
            val exito = productosRepository.eliminarProducto(producto.idProducto)
            if (exito) {
                Toast.makeText(this, "\"${producto.nombreProducto}\" fue desactivado", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                cargarDatos()
            } else {
                Toast.makeText(this, "No se pudo desactivar el producto", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    // =========================================================
    // UTILIDADES
    // =========================================================
    private fun formatearMonto(valor: Double): String {
        return String.format(Locale.getDefault(), "%,.2f", valor)
    }

    companion object {
        /** Si viene en true, la pantalla abre directo en la pestaña "Alertas" (stock bajo). */
        const val EXTRA_ABRIR_ALERTAS = "extra_abrir_alertas"
    }
}