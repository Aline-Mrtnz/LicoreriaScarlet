package com.example.scarlet

import com.example.scarlet.util.NavegacionOrigen

import android.content.Intent

import androidx.appcompat.app.AlertDialog

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.scarlet.data.repository.CuentaRepository
import com.example.scarlet.util.Session
import com.example.scarlet.util.ValidacionesBolivia

class MiCuenta : AppCompatActivity() {

    private lateinit var edtNombres: EditText
    private lateinit var edtApellidos: EditText
    private lateinit var edtUsuario: EditText
    private lateinit var edtCorreo: EditText
    private lateinit var edtTelefono: EditText
    private lateinit var edtContrasena: EditText

    private lateinit var txtNombreCuenta: TextView
    private lateinit var txtRolCuenta: TextView

    private lateinit var cuentaRepository: CuentaRepository

    private var idCuentaActual = -1
    private var idPersonaActual = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContentView(R.layout.activity_mi_cuenta)
        findViewById<ImageView>(R.id.btnBack).setOnClickListener { NavegacionOrigen.volverAOrigen(this) }

        // ============================================
        // REFERENCIAS
        // ============================================

        edtNombres = findViewById(R.id.edtNombres)
        edtApellidos = findViewById(R.id.edtApellidos)
        edtUsuario = findViewById(R.id.edtUsuario)
        edtCorreo = findViewById(R.id.edtCorreo)
        edtTelefono = findViewById(R.id.edtTelefono)
        edtContrasena = findViewById(R.id.edtContrasena)

        txtNombreCuenta =
            findViewById(R.id.txtNombreCuenta)

        txtRolCuenta =
            findViewById(R.id.txtRolCuenta)

        val btnBack =
            findViewById<ImageView>(R.id.btnBack)

        val btnGuardar =
            findViewById<Button>(R.id.btnGuardarCambios)

        cuentaRepository =
            CuentaRepository(this)

        // ============================================
        // EDGE TO EDGE
        // ============================================

        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.main)
        ) { v, insets ->

            val systemBars =
                insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                )

            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )

            insets
        }

        // ============================================
        // VOLVER
        // ============================================

        btnBack.setOnClickListener {
            NavegacionOrigen.volverAOrigen(this)
        }

        // ============================================
        // CARGAR DATOS REALES
        // ============================================

        cargarDatos()

        // ============================================
        // GUARDAR
        // ============================================

        btnGuardar.setOnClickListener {
            guardarCambios()
        }

        val imgMenu = findViewById<ImageView>(R.id.imgMenu)
        val sideMenu = findViewById<LinearLayout>(R.id.sideMenu)
        val menuOverlay = findViewById<View>(R.id.viewMenuOverlay)
        val menuProveedores = findViewById<TextView>(R.id.menuProveedores)
        val menuMiCuenta = findViewById<TextView>(R.id.menuMiCuenta)

        fun abrirMenu() {
            menuOverlay.visibility = View.VISIBLE
            sideMenu.visibility = View.VISIBLE
            sideMenu.translationX = -sideMenu.width.toFloat()
            sideMenu.animate().translationX(0f).setDuration(250).start()
            resaltarItemMenuActual()
        }

        fun cerrarMenu() {
            sideMenu.animate()
                .translationX(-sideMenu.width.toFloat())
                .setDuration(200)
                .withEndAction {
                    sideMenu.visibility = View.GONE
                    menuOverlay.visibility = View.GONE
                }
                .start()
        }

        imgMenu.setOnClickListener {
            if (sideMenu.visibility == View.GONE) abrirMenu() else cerrarMenu()
        }

        // Cualquier toque fuera del menú (en el resto de la pantalla) lo cierra
        menuOverlay.setOnClickListener { cerrarMenu() }

        // para el mi cuenta
        menuMiCuenta.setOnClickListener {
            cerrarMenu()
            NavegacionOrigen.cambiarModulo(this, MiCuenta::class.java)
        }
        // para caja
        findViewById<TextView>(R.id.menuCaja).setOnClickListener {
            cerrarMenu()
            NavegacionOrigen.cambiarModulo(this, CajaActivity::class.java)
        }
        // para proveedores
        menuProveedores.setOnClickListener {
            cerrarMenu()
            NavegacionOrigen.cambiarModulo(this, Proveedores::class.java)
        }
        // para categorías
        findViewById<TextView>(R.id.menuCategorias).setOnClickListener {
            cerrarMenu()
            NavegacionOrigen.cambiarModulo(this, CategoriasActivity::class.java)
        }
        // para inventario
        findViewById<TextView>(R.id.menuInventario).setOnClickListener {
            cerrarMenu()
            NavegacionOrigen.cambiarModulo(this, Inventario::class.java)
        }
        // para reabastecimiento / compras
        findViewById<TextView>(R.id.menuReabastecimiento).setOnClickListener {
            cerrarMenu()
            NavegacionOrigen.cambiarModulo(this, Reabastecimiento::class.java)
        }
        // para cuentas de cajero
        findViewById<TextView>(R.id.menuCajeros).setOnClickListener {
            cerrarMenu()
            NavegacionOrigen.cambiarModulo(this, GestionCajeros::class.java)
        }
        // Restringe accesos de gestión a solo el rol Administrador.
        if (!Session.esAdmin) {
            findViewById<TextView>(R.id.menuCategorias).visibility = View.GONE
            menuProveedores.visibility = View.GONE
            findViewById<TextView>(R.id.menuInventario).visibility = View.GONE
            findViewById<TextView>(R.id.menuReabastecimiento).visibility = View.GONE
            findViewById<TextView>(R.id.menuCajeros).visibility = View.GONE
        }
        // cerrar sesión
        findViewById<TextView>(R.id.menuSalir).setOnClickListener {
            cerrarMenu()
            AlertDialog.Builder(this)
                .setTitle("Cerrar sesión")
                .setMessage("¿Estás seguro de que deseas cerrar sesión?")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Salir") { _, _ ->
                    Session.cerrar()
                    com.example.scarlet.cart.CartManager.limpiar()
                    val intent = Intent(this, Login::class.java)
                    intent.flags =
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
                .show()
        }
    }
    private fun resaltarItemMenuActual() {
        // Mapea cada item del menú con la Activity a la que navega.
        // null = no navega a otra Activity (ej. "Salir"), nunca se resalta.
        val items = listOf(
            findViewById<TextView>(R.id.menuMiCuenta) to MiCuenta::class.java,
            findViewById<TextView>(R.id.menuCaja) to CajaActivity::class.java,
            findViewById<TextView>(R.id.menuCategorias) to CategoriasActivity::class.java,
            findViewById<TextView>(R.id.menuProveedores) to Proveedores::class.java,
            findViewById<TextView>(R.id.menuInventario) to Inventario::class.java,
            findViewById<TextView>(R.id.menuReabastecimiento) to Reabastecimiento::class.java,
            findViewById<TextView>(R.id.menuCajeros) to GestionCajeros::class.java
        )

        items.forEach { (item, clase) ->
            val esActual = clase == this::class.java
            item.setBackgroundResource(
                if (esActual) R.drawable.bg_menu_item_selected else android.R.color.transparent
            )
            item.setTextColor(if (esActual) 0xFFFF3B16.toInt() else 0xFFCCCCCC.toInt())
        }
    }
    // ============================================
    // CARGAR DATOS DEL USUARIO LOGUEADO
    // ============================================

    private fun cargarDatos() {

        try {

            val usuarioActual =
                cuentaRepository.obtenerUsuarioActual()

            if (usuarioActual == null) {

                Toast.makeText(
                    this,
                    "No se encontró la cuenta actual",
                    Toast.LENGTH_LONG
                ).show()

                return
            }

            // ========================================
            // GUARDAR IDs REALES
            // ========================================

            idCuentaActual =
                usuarioActual.idCuenta

            idPersonaActual =
                usuarioActual.idPersona

            // ========================================
            // MOSTRAR DATOS REALES
            // ========================================

            edtNombres.setText(
                usuarioActual.nombres
            )

            edtApellidos.setText(
                usuarioActual.apellidos
            )

            edtUsuario.setText(
                usuarioActual.usuario
            )

            edtTelefono.setText(
                usuarioActual.telefono
            )

            // ========================================
            // CORREO
            // ========================================
            // Tu tabla persona actual no tiene
            // una columna correo.
            // Por eso no podemos cargarlo desde BD.

            edtCorreo.setText("")

            // ========================================
            // CONTRASEÑA
            // ========================================
            // Nunca mostramos la contraseña actual.

            edtContrasena.setText("")

            // ========================================
            // INFORMACIÓN DEL PERFIL
            // ========================================

            txtNombreCuenta.text =
                "${usuarioActual.nombres} ${usuarioActual.apellidos}"

            txtRolCuenta.text =
                "● ${usuarioActual.nombreRol}"

        } catch (e: Exception) {

            e.printStackTrace()

            Toast.makeText(
                this,
                "Error al cargar los datos: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // ============================================
    // GUARDAR CAMBIOS
    // ============================================

    private fun guardarCambios() {

        val nombres =
            edtNombres.text.toString().trim()

        val apellidos =
            edtApellidos.text.toString().trim()

        val usuario =
            edtUsuario.text.toString().trim()

        val telefono =
            edtTelefono.text.toString().trim()

        val contrasena =
            edtContrasena.text.toString().trim()

        // ============================================
        // VALIDAR SESIÓN
        // ============================================

        if (idCuentaActual <= 0 ||
            idPersonaActual <= 0
        ) {

            Toast.makeText(
                this,
                "No se pudo identificar la cuenta",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        // ============================================
        // VALIDAR NOMBRES
        // ============================================

        if (nombres.isEmpty()) {

            edtNombres.error =
                "Ingresa tus nombres"

            edtNombres.requestFocus()

            return
        }

        // ============================================
        // VALIDAR APELLIDOS
        // ============================================

        if (apellidos.isEmpty()) {

            edtApellidos.error =
                "Ingresa tus apellidos"

            edtApellidos.requestFocus()

            return
        }

        // ============================================
        // VALIDAR USUARIO
        // ============================================

        if (usuario.isEmpty()) {

            edtUsuario.error =
                "Ingresa un nombre de usuario"

            edtUsuario.requestFocus()

            return
        }

        // ============================================
        // VALIDAR TELÉFONO
        // ============================================

        if (telefono.isEmpty()) {

            edtTelefono.error =
                "Ingresa tu teléfono"

            edtTelefono.requestFocus()

            return
        }

        if (!ValidacionesBolivia.esCelularValido(telefono)) {

            edtTelefono.error =
                ValidacionesBolivia.MENSAJE_CELULAR

            edtTelefono.requestFocus()

            return
        }

        // ============================================
        // ACTUALIZAR BD
        // ============================================

        try {

            val resultado =
                cuentaRepository.actualizarCuenta(
                    idCuenta = idCuentaActual,
                    idPersona = idPersonaActual,
                    nombres = nombres,
                    apellidos = apellidos,
                    usuario = usuario,
                    telefono = telefono,
                    nuevaClave = contrasena
                )

            if (resultado) {

                // ====================================
                // ACTUALIZAR SESSION
                // ====================================

                Session.iniciar(
                    idCuenta = idCuentaActual,
                    usuario = usuario,
                    nombreCompleto =
                        "$nombres $apellidos",
                    rol = Session.rol
                )

                // ====================================
                // ACTUALIZAR CABECERA
                // ====================================

                txtNombreCuenta.text =
                    "$nombres $apellidos"

                Toast.makeText(
                    this,
                    "Datos actualizados correctamente",
                    Toast.LENGTH_SHORT
                ).show()

                // No dejamos la contraseña escrita
                edtContrasena.setText("")

            } else {

                Toast.makeText(
                    this,
                    "No se pudo actualizar. Verifica el nombre de usuario.",
                    Toast.LENGTH_LONG
                ).show()
            }

        } catch (e: Exception) {

            e.printStackTrace()

            Toast.makeText(
                this,
                "Error al guardar: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}