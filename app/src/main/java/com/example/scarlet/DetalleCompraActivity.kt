package com.example.scarlet

import android.app.AlertDialog
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
import com.example.scarlet.data.model.PagoCompra
import com.example.scarlet.data.repository.ComprasRepository
import com.example.scarlet.util.FechaUtils
import com.example.scarlet.util.Session
import java.util.Locale

class DetalleCompraActivity : AppCompatActivity() {

    private lateinit var comprasRepository: ComprasRepository
    private var idCompra: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle_compra)

        if (!Session.esAdmin) {
            Toast.makeText(this, "Acceso solo para administradores", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        comprasRepository = ComprasRepository(this)
        idCompra = intent.getIntExtra(EXTRA_ID_COMPRA, -1)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<TextView>(R.id.btnCerrarDetalle).setOnClickListener { finish() }

        if (idCompra <= 0) {
            Toast.makeText(this, "No se pudo abrir la compra", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        cargarDatos()
    }

    override fun onResume() {
        super.onResume()
        if (idCompra > 0) cargarDatos()
    }

    private fun cargarDatos() {
        val compra = comprasRepository.obtenerCompra(idCompra)
        if (compra == null) {
            Toast.makeText(this, "La compra ya no existe", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        findViewById<TextView>(R.id.txtProveedorDetalle).text = compra.razonSocialProveedor ?: "-"
        findViewById<TextView>(R.id.txtFechaDetalle).text = ComprasAdapter.formatearFecha(compra.fechaEmision)
        findViewById<TextView>(R.id.txtNitDetalle).text = compra.rfcNitProveedor ?: "-"
        findViewById<TextView>(R.id.txtTotalDetalle).text = ComprasAdapter.formatearBs(compra.total)

        val txtEstado = findViewById<TextView>(R.id.txtEstadoDetalle)
        val layoutAcciones = findViewById<LinearLayout>(R.id.layoutAccionesPendiente)
        val txtMensajeRecibida = findViewById<TextView>(R.id.txtMensajeRecibida)
        val txtMensajeAnulada = findViewById<TextView>(R.id.txtMensajeAnulada)

        layoutAcciones.visibility = View.GONE
        txtMensajeRecibida.visibility = View.GONE
        txtMensajeAnulada.visibility = View.GONE

        when (compra.estado) {
            "RECIBIDA" -> {
                txtEstado.text = "RECIBIDA"
                txtEstado.setBackgroundResource(R.drawable.bg_pill_recibida)
                txtEstado.setTextColor(0xFF4CAF50.toInt())
                txtMensajeRecibida.visibility = View.VISIBLE
                txtMensajeRecibida.text =
                    "Esta compra ya fue recibida; su stock quedó sumado al inventario y registrado en el kárdex de Scarlet Licorería."
            }
            "ANULADA" -> {
                txtEstado.text = "ANULADA"
                txtEstado.setBackgroundResource(R.drawable.bg_pill_anulada)
                txtEstado.setTextColor(0xFFFF5252.toInt())
                txtMensajeAnulada.visibility = View.VISIBLE
                txtMensajeAnulada.text = "Esta compra fue anulada y no afectó el inventario."
            }
            else -> {
                txtEstado.text = "PENDIENTE"
                txtEstado.setBackgroundResource(R.drawable.bg_pill_pendiente)
                txtEstado.setTextColor(0xFFFFC107.toInt())
                layoutAcciones.visibility = View.VISIBLE
            }
        }

        findViewById<TextView>(R.id.btnMarcarRecibida).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Marcar como recibida")
                .setMessage("El stock de los productos de esta orden se sumará al inventario y quedará registrado en el kárdex. ¿Continuar?")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Confirmar") { _, _ ->
                    val usuario = if (Session.estaLogueado) Session.usuario else "admin"
                    if (comprasRepository.marcarRecibida(idCompra, usuario)) {
                        Toast.makeText(this, "Compra recibida, stock actualizado", Toast.LENGTH_SHORT).show()
                        cargarDatos()
                    } else {
                        Toast.makeText(this, "No se pudo actualizar la compra", Toast.LENGTH_SHORT).show()
                    }
                }
                .show()
        }

        findViewById<TextView>(R.id.btnAnularCompra).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Anular compra")
                .setMessage("¿Seguro que deseas anular esta orden de compra?")
                .setNegativeButton("No", null)
                .setPositiveButton("Sí, anular") { _, _ ->
                    if (comprasRepository.anularCompra(idCompra)) {
                        Toast.makeText(this, "Compra anulada", Toast.LENGTH_SHORT).show()
                        cargarDatos()
                    } else {
                        Toast.makeText(this, "No se pudo anular la compra", Toast.LENGTH_SHORT).show()
                    }
                }
                .show()
        }

        cargarPagos(compra)
        cargarProductosIngresados(compra)
    }

    private fun cargarPagos(compra: com.example.scarlet.data.model.Compra) {
        findViewById<TextView>(R.id.txtPagado).text =
            "Pagado: ${ComprasAdapter.formatearBs(compra.totalPagado)} / ${ComprasAdapter.formatearBs(compra.total)}"

        val txtSaldo = findViewById<TextView>(R.id.txtSaldoPendiente)
        txtSaldo.text = "Saldo Pendiente: ${ComprasAdapter.formatearBs(compra.saldoPendiente)}"
        txtSaldo.setTextColor(if (compra.saldoPendiente > 0.009) 0xFFFF5252.toInt() else 0xFF4CAF50.toInt())

        val llPagos = findViewById<LinearLayout>(R.id.llPagos)
        llPagos.removeAllViews()
        val pagos = comprasRepository.listarPagos(idCompra)
        pagos.forEach { pago ->
            val fila = LayoutInflater.from(this).inflate(R.layout.item_pago_compra, llPagos, false)
            fila.findViewById<TextView>(R.id.txtFechaPago).text = ComprasAdapter.formatearFecha(pago.fecha)
            fila.findViewById<TextView>(R.id.txtMetodoPago).text = pago.metodoPago
            fila.findViewById<TextView>(R.id.txtMontoPago).text = ComprasAdapter.formatearBs(pago.monto)
            fila.findViewById<TextView>(R.id.txtPorPago).text = pago.registradoPor ?: "-"
            llPagos.addView(fila)
        }

        val layoutRegistrarPago = findViewById<LinearLayout>(R.id.layoutRegistrarPago)
        val puedeAbonar = compra.estado != "ANULADA" && compra.saldoPendiente > 0.009
        layoutRegistrarPago.visibility = if (puedeAbonar) View.VISIBLE else View.GONE

        if (puedeAbonar) {
            val spinnerMetodo = findViewById<Spinner>(R.id.spinnerMetodoPago)
            spinnerMetodo.adapter = ArrayAdapter(
                this, R.layout.spinner_item_selected, listOf("Efectivo", "Transferencia", "QR")
            ).apply { setDropDownViewResource(R.layout.spinner_item_dropdown) }

            val btnRegistrarPago = findViewById<TextView>(R.id.btnRegistrarPago)
            btnRegistrarPago.setOnClickListener {
                val edtMonto = findViewById<EditText>(R.id.edtMontoPago)
                val edtEfectivo = findViewById<EditText>(R.id.edtEfectivoRecibido)
                val edtObservacion = findViewById<EditText>(R.id.edtObservacionPago)

                val monto = edtMonto.text.toString().toDoubleOrNull()
                if (monto == null || monto <= 0) {
                    Toast.makeText(this, "Ingresa un monto válido", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (monto > compra.saldoPendiente + 0.009) {
                    Toast.makeText(this, "El monto supera el saldo pendiente", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val pago = PagoCompra(
                    fecha = FechaUtils.ahora(),
                    metodoPago = spinnerMetodo.selectedItem.toString(),
                    monto = monto,
                    efectivoRecibido = edtEfectivo.text.toString().toDoubleOrNull(),
                    observacion = edtObservacion.text.toString().trim().ifBlank { null },
                    idCompra = idCompra,
                    registradoPor = if (Session.estaLogueado) Session.usuario else "admin"
                )

                if (comprasRepository.registrarPago(pago) > 0) {
                    Toast.makeText(this, "Abono registrado", Toast.LENGTH_SHORT).show()
                    edtMonto.setText("")
                    edtEfectivo.setText("")
                    edtObservacion.setText("")
                    cargarDatos()
                } else {
                    Toast.makeText(this, "No se pudo registrar el abono", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun cargarProductosIngresados(compra: com.example.scarlet.data.model.Compra) {
        val detalle = comprasRepository.listarDetalle(idCompra)

        findViewById<TextView>(R.id.txtCantidadItems).text =
            "${detalle.size} item${if (detalle.size == 1) "" else "s"}"

        val llProductos = findViewById<LinearLayout>(R.id.llProductosIngresados)
        llProductos.removeAllViews()
        detalle.forEach { item ->
            val fila = LayoutInflater.from(this).inflate(R.layout.item_producto_ingresado, llProductos, false)
            fila.findViewById<TextView>(R.id.txtNombreIngresado).text = item.nombreProducto ?: "-"
            fila.findViewById<TextView>(R.id.txtCantidadPrecioIngresado).text =
                "${item.cantidad} unidades  ·  Bs ${String.format(Locale("es", "BO"), "%,.2f", item.precioUnitario)}/c.u."
            fila.findViewById<TextView>(R.id.txtSubtotalIngresado).text = ComprasAdapter.formatearBs(item.subtotal)
            llProductos.addView(fila)
        }

        val base = compra.total / 1.13
        val iva = base * 0.13
        val it = base * 0.03
        findViewById<TextView>(R.id.txtImporteBase).text = ComprasAdapter.formatearBs(base)
        findViewById<TextView>(R.id.txtIva).text = ComprasAdapter.formatearBs(iva)
        findViewById<TextView>(R.id.txtIt).text = ComprasAdapter.formatearBs(it)
        findViewById<TextView>(R.id.txtTotalFacturado).text = ComprasAdapter.formatearBs(compra.total)
    }

    companion object {
        const val EXTRA_ID_COMPRA = "extra_id_compra"
    }
}