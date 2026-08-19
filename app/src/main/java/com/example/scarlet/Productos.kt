package com.example.scarlet

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class Productos : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_products)

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

        // Configurar listeners para los botones de agregar
        setupAddButtons()

        // Configurar listeners para los botones de descripción
        setupDescriptionButtons()

        // Configurar buscador
        setupSearch()

        // Configurar menú lateral
        setupMenu()

        // Configurar categorías
        setupCategories()

        setupCarrito()
    }

    private fun setupAddButtons() {
        val productIds = listOf(
            R.id.btnAgregarMacallan to "Macallan 18",
            R.id.btnAgregarDonJulio to "Don Julio 70",
            R.id.btnAgregarHennessy to "Hennessy X.O",
            R.id.btnAgregarGreyGoose to "Grey Goose",
            R.id.btnAgregarCasamigos to "Casamigos",
            R.id.btnAgregarVeuve to "Veuve Clicquot"
        )

        productIds.forEach { (id, name) ->
            findViewById<Button>(id).setOnClickListener {
                Toast.makeText(this, "$name agregado al carrito", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun setupCarrito() {
        val carritoIcon = findViewById<ImageView>(R.id.imgCarrito)
        carritoIcon.setOnClickListener {
            val intent = Intent(this, Shopping::class.java)
            startActivity(intent)
        }
    }
    private fun setupDescriptionButtons() {
        val descMap = mapOf(
            R.id.btnDescripcionMacallan to "Macallan 18 Sherry Oak\nUn whisky escocés espectacular con ricas notas de frutas secas, jengibre y especias de roble.",
            R.id.btnDescripcionDonJulio to "Don Julio 70 Añejo Claro\nUn tequila suave con notas de vainilla, caramelo y roble tostado.",
            R.id.btnDescripcionHennessy to "Hennessy X.O Cognac\nUn cognac premium con notas de frutas confitadas, chocolate y especias.",
            R.id.btnDescripcionGreyGoose to "Grey Goose Vodka\nVodka premium francés elaborado con trigo de invierno y agua de manantial.",
            R.id.btnDescripcionCasamigos to "Casamigos Reposado\nTequila reposado con notas de caramelo, chocolate y roble suave.",
            R.id.btnDescripcionVeuve to "Veuve Clicquot Brut\nChampagne elegante con notas de manzana, pera y brioche."
        )

        descMap.forEach { (id, desc) ->
            findViewById<Button>(id).setOnClickListener {
                Toast.makeText(this, desc, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupSearch() {
        val searchEditText = findViewById<EditText>(R.id.edtBuscar)
        searchEditText.setOnEditorActionListener { _, _, _ ->
            val query = searchEditText.text.toString()
            if (query.isNotEmpty()) {
                Toast.makeText(this, "Buscando: $query", Toast.LENGTH_SHORT).show()
            }
            true
        }
    }

    private fun setupMenu() {
        val menuIcon = findViewById<ImageView>(R.id.imgMenu)
        menuIcon.setOnClickListener {
            Toast.makeText(this, "Menú lateral abierto", Toast.LENGTH_SHORT).show()
            // Aquí puedes abrir un Navigation Drawer o un Dialog
        }
    }

    private fun setupCategories() {
        val categoryIds = listOf(
            R.id.catTodos to "Todos",
            R.id.catWhisky to "Whisky",
            R.id.catTequila to "Tequila",
            R.id.catCognac to "Cognac",
            R.id.catVodka to "Vodka",
            R.id.catChampagne to "Champagne",
            //R.id.catRon to "Ron",
            //R.id.catGin to "Gin"
        )

        var selectedView: TextView? = findViewById(R.id.catTodos)

        categoryIds.forEach { (id, name) ->
            val view = findViewById<TextView>(id)
            view.setOnClickListener {
                // Resetear estilo del anterior seleccionado
                selectedView?.apply {
                    setBackgroundResource(R.drawable.bg_category_unselected)
                    setTextColor(resources.getColor(android.R.color.darker_gray))
                }

                // Establecer nuevo seleccionado
                view.apply {
                    setBackgroundResource(R.drawable.bg_category_selected)
                    setTextColor(resources.getColor(android.R.color.white))
                }
                selectedView = view

                Toast.makeText(this, "Filtrando por: $name", Toast.LENGTH_SHORT).show()
            }
        }
    }
}