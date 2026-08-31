package com.example.scarlet

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.scarlet.data.model.Categorias
import com.example.scarlet.data.model.Producto
import com.example.scarlet.data.repository.CategoriasRepository
import com.example.scarlet.data.repository.ProductosRepository
import java.io.File
import java.io.FileOutputStream

class AgregarProducto : AppCompatActivity() {

    private lateinit var productosRepository: ProductosRepository
    private lateinit var categoriasRepository: CategoriasRepository

    private lateinit var frameImagenProducto: FrameLayout
    private lateinit var layoutPlaceholder: View
    private lateinit var imgPreview: ImageView

    private lateinit var edtNombre: EditText
    private lateinit var spinnerCategoria: Spinner
    private lateinit var spinnerVolumen: Spinner
    private lateinit var edtPrecio: EditText
    private lateinit var edtStock: EditText
    private lateinit var edtAbv: EditText
    private lateinit var edtDescripcion: EditText

    private var categorias: List<Categorias> = emptyList()
    private var rutaImagenGuardada: String? = null

    // Opciones fijas de volumen de botella (en ml)
    private val volumenes = listOf(200, 375, 500, 700, 750, 1000, 1750)

    private val seleccionarImagenLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                manejarImagenSeleccionada(uri)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_product)

        productosRepository = ProductosRepository(this)
        categoriasRepository = CategoriasRepository(this)

        vincularVistas()
        configurarImagen()
        configurarSpinnerCategoria()
        configurarSpinnerVolumen()
        configurarBotones()
    }

    private fun vincularVistas() {
        frameImagenProducto = findViewById(R.id.frameImagenProducto)
        layoutPlaceholder = findViewById(R.id.layoutSubirImagenPlaceholder)
        imgPreview = findViewById(R.id.imgPreviewProducto)

        edtNombre = findViewById(R.id.edtNombreProducto)
        spinnerCategoria = findViewById(R.id.spinnerCategoria)
        spinnerVolumen = findViewById(R.id.spinnerVolumen)
        edtPrecio = findViewById(R.id.edtPrecio)
        edtStock = findViewById(R.id.edtStock)
        edtAbv = findViewById(R.id.edtAbv)
        edtDescripcion = findViewById(R.id.edtDescripcion)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }

    private fun configurarImagen() {
        frameImagenProducto.setOnClickListener {
            seleccionarImagenLauncher.launch("image/*")
        }
    }

    private fun manejarImagenSeleccionada(uri: Uri) {
        try {
            val bitmap = contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input)
            } ?: return

            val archivo = guardarImagenEnAlmacenamientoInterno(bitmap)
            rutaImagenGuardada = archivo.absolutePath

            imgPreview.setImageBitmap(bitmap)
            imgPreview.visibility = View.VISIBLE
            layoutPlaceholder.visibility = View.GONE
        } catch (e: Exception) {
            Toast.makeText(this, "No se pudo cargar la imagen seleccionada", Toast.LENGTH_SHORT).show()
        }
    }

    private fun guardarImagenEnAlmacenamientoInterno(bitmap: Bitmap): File {
        val carpetaProductos = File(filesDir, "productos_img").apply { mkdirs() }
        val archivo = File(carpetaProductos, "producto_${System.currentTimeMillis()}.jpg")
        FileOutputStream(archivo).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }
        return archivo
    }

    private fun configurarSpinnerCategoria() {
        categorias = categoriasRepository.listar()

        if (categorias.isEmpty()) {
            spinnerCategoria.adapter = crearAdapterSpinner(listOf("Sin categorías disponibles"))
            spinnerCategoria.isEnabled = false
            return
        }

        val nombres = listOf("Seleccionar categoría...") + categorias.map { it.nombre_categoria }
        spinnerCategoria.adapter = crearAdapterSpinner(nombres)
    }

    private fun configurarSpinnerVolumen() {
        val etiquetas = volumenes.map { formatearVolumen(it) }
        spinnerVolumen.adapter = crearAdapterSpinner(etiquetas)

        // Preseleccionamos 750ml, como en el diseño de referencia
        val indicePorDefecto = volumenes.indexOf(750)
        if (indicePorDefecto >= 0) {
            spinnerVolumen.setSelection(indicePorDefecto)
        }
    }

    private fun formatearVolumen(ml: Int): String {
        return if (ml >= 1000) {
            val litros = ml / 1000.0
            if (litros == litros.toInt().toDouble()) "${litros.toInt()}L" else "${litros}L"
        } else {
            "${ml}ml"
        }
    }

    private fun crearAdapterSpinner(items: List<String>): ArrayAdapter<String> {
        return ArrayAdapter(this, R.layout.spinner_item_selected, items).apply {
            setDropDownViewResource(R.layout.spinner_item_dropdown)
        }
    }

    private fun configurarBotones() {
        findViewById<TextView>(R.id.btnCancelar).setOnClickListener {
            finish()
        }

        findViewById<View>(R.id.btnGuardarProducto).setOnClickListener {
            guardarProducto()
        }
    }

    private fun guardarProducto() {
        val nombre = edtNombre.text.toString().trim()
        val precioTexto = edtPrecio.text.toString().trim()
        val stockTexto = edtStock.text.toString().trim()
        val abvTexto = edtAbv.text.toString().trim()
        val descripcion = edtDescripcion.text.toString().trim()

        if (nombre.isEmpty()) {
            Toast.makeText(this, "Ingresa el nombre del producto", Toast.LENGTH_SHORT).show()
            return
        }

        if (categorias.isEmpty() || spinnerCategoria.selectedItemPosition <= 0) {
            Toast.makeText(this, "Selecciona una categoría", Toast.LENGTH_SHORT).show()
            return
        }

        val precio = precioTexto.toDoubleOrNull()
        if (precio == null || precio <= 0) {
            Toast.makeText(this, "Ingresa un precio válido", Toast.LENGTH_SHORT).show()
            return
        }

        val stock = stockTexto.toIntOrNull()
        if (stock == null || stock < 0) {
            Toast.makeText(this, "Ingresa un stock válido", Toast.LENGTH_SHORT).show()
            return
        }

        val abv = if (abvTexto.isEmpty()) null else abvTexto.toDoubleOrNull()
        if (abvTexto.isNotEmpty() && abv == null) {
            Toast.makeText(this, "Ingresa un valor de ABV válido", Toast.LENGTH_SHORT).show()
            return
        }

        // La posición 0 es "Seleccionar categoría...", por eso restamos 1
        val categoriaSeleccionada = categorias[spinnerCategoria.selectedItemPosition - 1]
        val volumenSeleccionado = volumenes.getOrNull(spinnerVolumen.selectedItemPosition)

        val nuevoProducto = Producto(
            idProducto = 0,
            nombreProducto = nombre,
            descripcion = descripcion.ifEmpty { null },
            imagen = rutaImagenGuardada,
            precioVenta = precio,
            precioMayor = null,
            precioCompra = null,
            stock = stock,
            stockMinimo = 5,
            estado = "ACTIVO",
            idCategoria = categoriaSeleccionada.id_categoria,
            marcasIdMarca = null,
            volumenMl = volumenSeleccionado,
            abv = abv
        )

        val id = productosRepository.crearProducto(nuevoProducto)
        if (id > 0) {
            Toast.makeText(this, "Producto \"$nombre\" guardado correctamente", Toast.LENGTH_SHORT).show()
            setResult(RESULT_OK)
            finish()
        } else {
            Toast.makeText(this, "No se pudo guardar el producto", Toast.LENGTH_SHORT).show()
        }
    }
}
