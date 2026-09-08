package com.example.scarlet

import com.example.scarlet.util.NavegacionOrigen

import android.content.Intent

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.scarlet.data.repository.CuentaRepository
import com.example.scarlet.data.repository.ResultadoTurno
import com.example.scarlet.data.repository.TurnoCajaRepository
import com.example.scarlet.util.Session
import java.util.Locale

class CajaActivity : AppCompatActivity() {

    private lateinit var turnoCajaRepository: TurnoCajaRepository
    private var idCuentaActual = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_caja)
        findViewById<ImageView>(R.id.btnBack).setOnClickListener { NavegacionOrigen.volverAOrigen(this) }

        turnoCajaRepository = TurnoCajaRepository(this)
        idCuentaActual = if (Session.estaLogueado) Session.idCuenta
        else CuentaRepository(this).obtenerUsuarioActual()?.idCuenta ?: -1

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { NavegacionOrigen.volverAOrigen(this) }
        findViewById<TextView>(R.id.btnAbrirCaja).setOnClickListener { abrirCaja() }
        findViewById<TextView>(R.id.btnCerrarCaja).setOnClickListener { confirmarCierre() }

        val imgMenu = findViewById<ImageView>(R.id.imgMenu)
        val sideMenu = findViewById<LinearLayout>(R.id.sideMenu)
        val menuOverlay = findViewById<View>(R.id.viewMenuOverlay)
        val menuProveedores = findViewById<TextView>(R.id.menuProveedores)
        val menuMiCuenta = findViewById<TextView>(R.id.menuMiCuenta)

        fun abrirMenu() {
            menuOverlay.visibility = View.VISIBLE
            sideMenu.visibility = View.VISIBLE
            resaltarItemMenuActual()
            // Se espera al siguiente frame para que sideMenu ya tenga su
            // ancho medido (si estaba GONE, width valía 0 y el menú no
            // se deslizaba desde fuera de la pantalla).
            sideMenu.post {
                sideMenu.translationX = -sideMenu.width.toFloat()
                sideMenu.animate().translationX(0f).setDuration(250).start()
            }
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

    override fun onResume() {
        super.onResume()
        actualizarEstado()
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
    private fun actualizarEstado() {
        if (idCuentaActual <= 0) {
            Toast.makeText(this, "Inicia sesión para gestionar la caja", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val layoutCerrada = findViewById<LinearLayout>(R.id.layoutCajaCerrada)
        val layoutAbierta = findViewById<LinearLayout>(R.id.layoutCajaAbierta)

        val turno = turnoCajaRepository.turnoAbierto(idCuentaActual)
        if (turno == null) {
            layoutCerrada.visibility = android.view.View.VISIBLE
            layoutAbierta.visibility = android.view.View.GONE
        } else {
            layoutCerrada.visibility = android.view.View.GONE
            layoutAbierta.visibility = android.view.View.VISIBLE
            findViewById<TextView>(R.id.txtInfoTurno).text =
                "Abierta el ${turno.fechaApertura}\nMonto de apertura: Bs ${"%,.2f".format(Locale("es", "BO"), turno.montoApertura)}"
        }
    }

    private fun abrirCaja() {
        val monto = findViewById<EditText>(R.id.edtMontoApertura).text.toString().toDoubleOrNull()
        if (monto == null || monto < 0) {
            Toast.makeText(this, "Ingresa un monto de apertura válido", Toast.LENGTH_SHORT).show()
            return
        }
        when (val resultado = turnoCajaRepository.abrirTurno(idCuentaActual, monto)) {
            is ResultadoTurno.Exito -> {
                Toast.makeText(this, "Caja abierta", Toast.LENGTH_SHORT).show()
                actualizarEstado()
            }
            is ResultadoTurno.YaHayTurnoAbierto -> Toast.makeText(this, "Ya tienes una caja abierta", Toast.LENGTH_SHORT).show()
            is ResultadoTurno.Error -> Toast.makeText(this, "Error: ${resultado.mensaje}", Toast.LENGTH_LONG).show()
            else -> {}
        }
    }

    private fun confirmarCierre() {
        val montoContado = findViewById<EditText>(R.id.edtMontoContado).text.toString().toDoubleOrNull()
        if (montoContado == null || montoContado < 0) {
            Toast.makeText(this, "Ingresa el efectivo contado", Toast.LENGTH_SHORT).show()
            return
        }
        val observacion = findViewById<EditText>(R.id.edtObservacionCierre).text.toString().trim()

        AlertDialog.Builder(this)
            .setTitle("Cerrar caja")
            .setMessage("Se calculará la diferencia contra lo esperado según las ventas en efectivo del turno. ¿Confirmar cierre?")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Confirmar") { _, _ -> cerrarCaja(montoContado, observacion) }
            .show()
    }

    private fun cerrarCaja(montoContado: Double, observacion: String) {
        when (val resultado = turnoCajaRepository.cerrarTurno(idCuentaActual, montoContado, observacion)) {
            is ResultadoTurno.Exito -> {
                val turno = turnoCajaRepository.listarHistorial(idCuentaActual).firstOrNull { it.idTurno == resultado.idTurno.toInt() }
                val diferencia = turno?.diferencia ?: 0.0
                val mensaje = when {
                    diferencia == 0.0 -> "Arqueo cuadrado, sin diferencias."
                    diferencia > 0 -> "Sobrante de Bs ${"%,.2f".format(diferencia)}"
                    else -> "Faltante de Bs ${"%,.2f".format(-diferencia)}"
                }
                AlertDialog.Builder(this)
                    .setTitle("Caja cerrada")
                    .setMessage(mensaje)
                    .setPositiveButton("Aceptar") { _, _ -> finish() }
                    .show()
            }
            is ResultadoTurno.NoHayTurnoAbierto -> Toast.makeText(this, "No hay una caja abierta", Toast.LENGTH_SHORT).show()
            is ResultadoTurno.Error -> Toast.makeText(this, "Error: ${resultado.mensaje}", Toast.LENGTH_LONG).show()
            else -> {}
        }
    }
}