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
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.scarlet.adapter.CategoriasAdapter
import com.example.scarlet.data.model.CategoriaConConteo
import com.example.scarlet.data.model.Categorias
import com.example.scarlet.data.model.Producto
import com.example.scarlet.data.repository.CategoriasRepository
import com.example.scarlet.data.repository.ProductosRepository
import com.example.scarlet.util.Session
import com.example.scarlet.util.ImagenUtils
import java.io.File
import java.io.FileOutputStream
import java.text.DecimalFormat

class CategoriasActivity : AppCompatActivity() {

    private enum class FiltroTab { TODAS, ACTIVAS, INACTIVAS }

    private lateinit var categoriasRepository: CategoriasRepository
    private lateinit var productosRepository: ProductosRepository

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CategoriasAdapter
    private lateinit var edtBuscar: EditText
    private lateinit var tabTodas: TextView
    private lateinit var tabActivas: TextView
    private lateinit var tabInactivas: TextView
    private lateinit var tvTotalCategorias: TextView
    private lateinit var emptyState: View

    private var listaCompleta: List<CategoriaConConteo> = emptyList()
    private var filtroActual = FiltroTab.TODAS
    private var textoBusqueda: String = ""

    private val decimalFormat = DecimalFormat("Bs #,##0.00")

    // ---- Estado del diálogo de foto (compartido entre crear/editar) ----
    private var dialogImgPreview: ImageView? = null
    private var dialogPlaceholder: View? = null
    private var dialogBtnQuitar: View? = null
    private var rutaImagenSeleccionada: String? = null

    private val seleccionarImagenLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) manejarImagenSeleccionada(uri)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_categorias)

        if (!Session.esAdmin) {
            Toast.makeText(this, "Acceso solo para administradores", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        categoriasRepository = CategoriasRepository(this)
        productosRepository = ProductosRepository(this)

        vincularVistas()
        configurarRecyclerView()
        configurarListeners()

        cargarCategorias()
    }

    override fun onResume() {
        super.onResume()
        cargarCategorias()
    }

    private fun vincularVistas() {
        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        recyclerView = findViewById(R.id.recyclerViewCategorias)
        edtBuscar = findViewById(R.id.edtBuscarCategoria)
        tabTodas = findViewById(R.id.tabTodas)
        tabActivas = findViewById(R.id.tabActivas)
        tabInactivas = findViewById(R.id.tabInactivas)
        tvTotalCategorias = findViewById(R.id.tvTotalCategorias)
        emptyState = findViewById(R.id.emptyStateCategorias)
    }

    private fun configurarRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = CategoriasAdapter(
            items = emptyList(),
            onToggleEstado = { item, activo -> cambiarEstado(item, activo) },
            onVerDetalles = { item -> mostrarDetallesProductos(item) },
            onEditar = { item -> mostrarDialogoFormulario(item.categoria) },
            onEliminar = { item -> confirmarEliminar(item) }
        )
        recyclerView.adapter = adapter
    }

    private fun configurarListeners() {
        findViewById<View>(R.id.btnAnadirCategoria).setOnClickListener {
            mostrarDialogoFormulario(null)
        }

        tabTodas.setOnClickListener { seleccionarTab(FiltroTab.TODAS) }
        tabActivas.setOnClickListener { seleccionarTab(FiltroTab.ACTIVAS) }
        tabInactivas.setOnClickListener { seleccionarTab(FiltroTab.INACTIVAS) }

        edtBuscar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                textoBusqueda = s?.toString()?.trim().orEmpty()
                aplicarFiltros()
            }
        })
    }

    private fun seleccionarTab(tab: FiltroTab) {
        filtroActual = tab
        val seleccionado = R.drawable.bg_filter_selected
        val noSeleccionado = R.drawable.bg_filter_unselected

        tabTodas.setBackgroundResource(if (tab == FiltroTab.TODAS) seleccionado else noSeleccionado)
        tabTodas.setTextColor(if (tab == FiltroTab.TODAS) 0xFFFFFFFF.toInt() else 0xFF777777.toInt())

        tabActivas.setBackgroundResource(if (tab == FiltroTab.ACTIVAS) seleccionado else noSeleccionado)
        tabActivas.setTextColor(if (tab == FiltroTab.ACTIVAS) 0xFFFFFFFF.toInt() else 0xFF777777.toInt())

        tabInactivas.setBackgroundResource(if (tab == FiltroTab.INACTIVAS) seleccionado else noSeleccionado)
        tabInactivas.setTextColor(if (tab == FiltroTab.INACTIVAS) 0xFFFFFFFF.toInt() else 0xFF777777.toInt())

        aplicarFiltros()
    }

    private fun cargarCategorias() {
        listaCompleta = categoriasRepository.listarConConteo()
        actualizarContadoresTabs()
        aplicarFiltros()
    }

    private fun actualizarContadoresTabs() {
        val total = listaCompleta.size
        val activas = listaCompleta.count { it.categoria.estaActiva }
        val inactivas = total - activas
        tabTodas.text = "Todas ($total)"
        tabActivas.text = "Activas ($activas)"
        tabInactivas.text = "Inactivas ($inactivas)"
    }

    private fun aplicarFiltros() {
        var lista = listaCompleta

        lista = when (filtroActual) {
            FiltroTab.TODAS -> lista
            FiltroTab.ACTIVAS -> lista.filter { it.categoria.estaActiva }
            FiltroTab.INACTIVAS -> lista.filter { !it.categoria.estaActiva }
        }

        if (textoBusqueda.isNotEmpty()) {
            val q = textoBusqueda.lowercase()
            lista = lista.filter {
                it.categoria.nombre_categoria.lowercase().contains(q) ||
                        it.categoria.listaEtiquetas().any { etiqueta -> etiqueta.lowercase().contains(q) } ||
                        it.categoria.descripcion.lowercase().contains(q)
            }
        }

        adapter.actualizar(lista)
        tvTotalCategorias.text = "${lista.size} en total"
        emptyState.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
        recyclerView.visibility = if (lista.isEmpty()) View.GONE else View.VISIBLE
    }

    // =========================================================
    // CAMBIAR ESTADO (switch de la lista)
    // =========================================================
    private fun cambiarEstado(item: CategoriaConConteo, activo: Boolean) {
        val nuevoEstado = if (activo) CategoriasRepository.ESTADO_ACTIVA else CategoriasRepository.ESTADO_INACTIVA
        categoriasRepository.actualizarEstado(item.categoria.id_categoria, nuevoEstado)
        val mensaje = if (activo)
            "\"${item.categoria.nombre_categoria}\" ahora está activa"
        else
            "\"${item.categoria.nombre_categoria}\" ahora está inactiva"
        Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
        cargarCategorias()
    }

    // =========================================================
    // VER DETALLES (productos de la categoría)
    // =========================================================
    private fun mostrarDetallesProductos(item: CategoriaConConteo) {
        val vista = LayoutInflater.from(this).inflate(R.layout.dialog_categoria_productos, null)
        val dialog = AlertDialog.Builder(this).setView(vista).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val productos = productosRepository.obtenerProductosPorIdCategoria(item.categoria.id_categoria)

        vista.findViewById<TextView>(R.id.tvTituloProductosCategoria).text =
            "Productos en «${item.categoria.nombre_categoria}»"
        vista.findViewById<TextView>(R.id.tvSubtituloProductosCategoria).text =
            "${productos.size} producto(s) usando esta categoría."

        val contenedor = vista.findViewById<LinearLayout>(R.id.llListaProductosCategoria)
        val vacio = vista.findViewById<TextView>(R.id.tvSinProductosCategoria)

        if (productos.isEmpty()) {
            contenedor.visibility = View.GONE
            vacio.visibility = View.VISIBLE
        } else {
            contenedor.visibility = View.VISIBLE
            vacio.visibility = View.GONE
            productos.forEach { producto -> contenedor.addView(crearFilaProducto(producto)) }
        }

        vista.findViewById<TextView>(R.id.btnCerrarProductosCategoria).setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    private fun crearFilaProducto(producto: Producto): View {
        val fila = LayoutInflater.from(this).inflate(R.layout.item_producto_categoria_row, null)
        fila.findViewById<TextView>(R.id.tvFilaProductoNombre).text = producto.nombreProducto
        fila.findViewById<TextView>(R.id.tvFilaProductoPrecio).text = decimalFormat.format(producto.precioVenta)
        fila.findViewById<TextView>(R.id.tvFilaProductoStock).text = producto.stock.toString()

        val tvEstado = fila.findViewById<TextView>(R.id.tvFilaProductoEstado)
        tvEstado.text = producto.estado
        if (producto.estado == "ACTIVO") {
            tvEstado.setBackgroundResource(R.drawable.bg_badge_activo)
            tvEstado.setTextColor(0xFF4CD964.toInt())
        } else {
            tvEstado.setBackgroundResource(R.drawable.bg_badge_inactivo)
            tvEstado.setTextColor(0xFF999999.toInt())
        }
        return fila
    }

    // =========================================================
    // CREAR / EDITAR (diálogo con formulario)
    // =========================================================
    private fun mostrarDialogoFormulario(categoriaExistente: Categorias?) {
        val vista = LayoutInflater.from(this).inflate(R.layout.dialog_categoria_form, null)
        val dialog = AlertDialog.Builder(this).setView(vista).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val tvTitulo = vista.findViewById<TextView>(R.id.tvTituloDialogo)
        val frameImagen = vista.findViewById<FrameLayout>(R.id.frameImagenCategoria)
        val placeholder = vista.findViewById<View>(R.id.layoutSubirImagenCategoriaPlaceholder)
        val imgPreview = vista.findViewById<ImageView>(R.id.imgPreviewCategoria)
        val btnQuitarImagen = vista.findViewById<TextView>(R.id.btnQuitarImagenCategoria)
        val edtNombre = vista.findViewById<EditText>(R.id.edtNombreCategoria)
        val edtEtiquetas = vista.findViewById<EditText>(R.id.edtEtiquetasCategoria)
        val switchDestacar = vista.findViewById<SwitchCompat>(R.id.switchDestacarCategoria)
        val edtOrden = vista.findViewById<EditText>(R.id.edtOrdenCategoria)
        val spinnerEstado = vista.findViewById<Spinner>(R.id.spinnerEstadoCategoria)

        // Estado de imagen para este diálogo en particular
        dialogImgPreview = imgPreview
        dialogPlaceholder = placeholder
        dialogBtnQuitar = btnQuitarImagen
        rutaImagenSeleccionada = categoriaExistente?.imagen_referencia

        val spinnerAdapter = ArrayAdapter(
            this,
            R.layout.spinner_item_selected,
            listOf("Activa (Visible)", "Inactiva (Oculta)")
        ).apply { setDropDownViewResource(R.layout.spinner_item_dropdown) }
        spinnerEstado.adapter = spinnerAdapter

        if (categoriaExistente != null) {
            tvTitulo.text = "Editar Categoría"
            edtNombre.setText(categoriaExistente.nombre_categoria)
            edtEtiquetas.setText(categoriaExistente.etiquetas.orEmpty())
            switchDestacar.isChecked = categoriaExistente.destacado
            edtOrden.setText(categoriaExistente.orden_menu.toString())
            spinnerEstado.setSelection(if (categoriaExistente.estaActiva) 0 else 1)
            mostrarPreviaImagen(categoriaExistente.imagen_referencia)
        } else {
            tvTitulo.text = "Nueva Categoría"
            edtOrden.setText(categoriasRepository.siguienteOrden().toString())
            spinnerEstado.setSelection(0)
            mostrarPreviaImagen(null)
        }

        frameImagen.setOnClickListener {
            seleccionarImagenLauncher.launch("image/*")
        }

        btnQuitarImagen.setOnClickListener {
            rutaImagenSeleccionada = null
            mostrarPreviaImagen(null)
        }

        vista.findViewById<ImageView>(R.id.btnCerrarDialogoCategoria).setOnClickListener { dialog.dismiss() }
        vista.findViewById<View>(R.id.btnCancelarCategoria).setOnClickListener { dialog.dismiss() }

        vista.findViewById<View>(R.id.btnGuardarCategoria).setOnClickListener {
            guardarCategoria(
                categoriaExistente = categoriaExistente,
                nombre = edtNombre.text.toString().trim(),
                etiquetas = edtEtiquetas.text.toString().trim(),
                destacado = switchDestacar.isChecked,
                ordenTexto = edtOrden.text.toString().trim(),
                estadoActivo = spinnerEstado.selectedItemPosition == 0,
                dialog = dialog
            )
        }

        dialog.show()
    }

    private fun mostrarPreviaImagen(ruta: String?) {
        val imgPreview = dialogImgPreview ?: return
        val placeholder = dialogPlaceholder ?: return
        val btnQuitar = dialogBtnQuitar

        if (!ruta.isNullOrBlank()) {
            ImagenUtils.cargarEnImageView(this, imgPreview, ruta)
            imgPreview.visibility = View.VISIBLE
            placeholder.visibility = View.GONE
            btnQuitar?.visibility = View.VISIBLE
        } else {
            imgPreview.visibility = View.GONE
            placeholder.visibility = View.VISIBLE
            btnQuitar?.visibility = View.GONE
        }
    }

    private fun manejarImagenSeleccionada(uri: Uri) {
        try {
            val bitmap = contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input)
            } ?: return

            val archivo = guardarImagenEnAlmacenamientoInterno(bitmap)
            rutaImagenSeleccionada = archivo.absolutePath
            mostrarPreviaImagen(rutaImagenSeleccionada)
        } catch (e: Exception) {
            Toast.makeText(this, "No se pudo cargar la imagen seleccionada", Toast.LENGTH_SHORT).show()
        }
    }

    private fun guardarImagenEnAlmacenamientoInterno(bitmap: Bitmap): File {
        val carpeta = File(filesDir, "categorias_img").apply { mkdirs() }
        val archivo = File(carpeta, "categoria_${System.currentTimeMillis()}.jpg")
        FileOutputStream(archivo).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }
        return archivo
    }

    private fun guardarCategoria(
        categoriaExistente: Categorias?,
        nombre: String,
        etiquetas: String,
        destacado: Boolean,
        ordenTexto: String,
        estadoActivo: Boolean,
        dialog: AlertDialog
    ) {
        if (nombre.isEmpty()) {
            Toast.makeText(this, "Ingresa el nombre de la categoría", Toast.LENGTH_SHORT).show()
            return
        }

        val nombreDuplicado = listaCompleta.any {
            it.categoria.nombre_categoria.equals(nombre, ignoreCase = true) &&
                    it.categoria.id_categoria != (categoriaExistente?.id_categoria ?: -1)
        }
        if (nombreDuplicado) {
            Toast.makeText(this, "Ya existe una categoría con ese nombre", Toast.LENGTH_SHORT).show()
            return
        }

        val orden = ordenTexto.toIntOrNull()
            ?: (categoriaExistente?.orden_menu ?: categoriasRepository.siguienteOrden())

        val categoria = Categorias(
            id_categoria = categoriaExistente?.id_categoria ?: 0,
            nombre_categoria = nombre,
            descripcion = categoriaExistente?.descripcion ?: "",
            imagen_referencia = rutaImagenSeleccionada,
            etiquetas = etiquetas,
            destacado = destacado,
            orden_menu = orden,
            estado = if (estadoActivo) CategoriasRepository.ESTADO_ACTIVA else CategoriasRepository.ESTADO_INACTIVA
        )

        val exito = if (categoriaExistente == null) {
            categoriasRepository.crear(categoria) > 0
        } else {
            categoriasRepository.editar(categoria) > 0
        }

        if (exito) {
            Toast.makeText(
                this,
                if (categoriaExistente == null) "Categoría \"$nombre\" creada correctamente. Aparecerá en el catálogo cuando tenga al menos un producto activo."
                else "Categoría \"$nombre\" actualizada correctamente",
                Toast.LENGTH_LONG
            ).show()
            dialog.dismiss()
            cargarCategorias()
        } else {
            Toast.makeText(this, "No se pudo guardar la categoría", Toast.LENGTH_SHORT).show()
        }
    }

    // =========================================================
    // ELIMINAR
    // =========================================================
    private fun confirmarEliminar(item: CategoriaConConteo) {
        val cantidad = categoriasRepository.contarProductos(item.categoria.id_categoria)

        if (cantidad > 0) {
            AlertDialog.Builder(this)
                .setTitle("No se puede eliminar")
                .setMessage(
                    "\"${item.categoria.nombre_categoria}\" tiene $cantidad producto(s) asociado(s). " +
                            "Reasigna o elimina esos productos antes de borrar la categoría."
                )
                .setPositiveButton("Entendido", null)
                .show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Eliminar categoría")
            .setMessage(
                "Esto BORRA la categoría \"${item.categoria.nombre_categoria}\" de forma permanente e irreversible.\n\n¿Confirmas que quieres eliminarla?"
            )
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Aceptar") { _, _ ->
                val filas = categoriasRepository.eliminar(item.categoria.id_categoria)
                if (filas > 0) {
                    Toast.makeText(this, "Categoría eliminada", Toast.LENGTH_SHORT).show()
                    cargarCategorias()
                } else {
                    Toast.makeText(
                        this,
                        "No se pudo eliminar la categoría (puede tener productos asociados)",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .show()
    }
}