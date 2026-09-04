package com.example.scarlet

import android.os.Bundle
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

        turnoCajaRepository = TurnoCajaRepository(this)
        idCuentaActual = if (Session.estaLogueado) Session.idCuenta
        else CuentaRepository(this).obtenerUsuarioActual()?.idCuenta ?: -1

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<TextView>(R.id.btnAbrirCaja).setOnClickListener { abrirCaja() }
        findViewById<TextView>(R.id.btnCerrarCaja).setOnClickListener { confirmarCierre() }
    }

    override fun onResume() {
        super.onResume()
        actualizarEstado()
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