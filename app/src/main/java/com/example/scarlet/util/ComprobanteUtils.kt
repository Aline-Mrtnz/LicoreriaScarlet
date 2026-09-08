package com.example.scarlet.util

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.scarlet.data.model.Compra
import com.example.scarlet.data.model.DetalleCompra
import com.example.scarlet.data.model.PagoCompra
import java.util.Locale
import com.example.scarlet.data.model.TopProducto

object ComprobanteUtils {
    // TODO: reemplaza con los datos reales de tu negocio
    private const val NOMBRE_NEGOCIO = "Licorería Scarlet"
    private const val DIRECCION = "Cochabamba, Bolivia"
    private const val NIT = "COMPLETAR-NIT"

    private fun bs(v: Double) = "Bs " + String.format(Locale("es", "BO"), "%,.2f", v)

    fun imprimirPagoCompra(
        context: Context,
        compra: Compra,
        pago: PagoCompra,
        numeroAbono: Int? = null
    ) {
        val saldo = compra.saldoPendiente
        val estadoSaldo = if (saldo <= 0.009)
            """<span style="color:#2e7d32;font-weight:bold">Bs 0,00 (saldada)</span>"""
        else
            """<span style="color:#c62828;font-weight:bold">${bs(saldo)}</span>"""

        val etiquetaAbono = if (numeroAbono != null) "Abono N.° $numeroAbono" else "Pago #${pago.idPagoCompra}"

        val html = """
    <html><body style="font-family:sans-serif;padding:28px;color:#222">
      <div style="display:flex;justify-content:space-between;border-bottom:2px solid #ddd;padding-bottom:14px">
        <div>
          <h2 style="margin:0">$NOMBRE_NEGOCIO</h2>
          <p style="margin:2px 0;color:#555">$DIRECCION<br/>NIT: $NIT</p>
        </div>
        <div style="text-align:right">
          <div style="border:1px solid #333;border-radius:6px;padding:8px 14px">
            <b>COMPROBANTE DE PAGO</b><br/>$etiquetaAbono
          </div>
        </div>
      </div>

      <p style="color:#777;margin-top:10px">
        Orden de compra ${compra.codigo} · ${compra.estado}
      </p>

      <table width="100%" style="margin-top:14px">
        <tr>
          <td><b>Fecha</b><br/>${pago.fecha}</td>
          <td><b>Proveedor</b><br/>${compra.razonSocialProveedor ?: "-"}</td>
          <td><b>Registrado por</b><br/>${pago.registradoPor ?: "-"}</td>
        </tr>
      </table>

      <table width="100%" style="margin-top:14px">
        <tr>
          <td><b>Método de pago</b><br/>${pago.metodoPago}</td>
          <td align="right"><b>Saldo pendiente de la orden</b><br/>$estadoSaldo</td>
        </tr>
      </table>

      <div style="background:#111;color:#fff;padding:12px 16px;display:flex;justify-content:space-between;margin-top:20px;border-radius:6px">
        <b>MONTO PAGADO</b><b>${bs(pago.monto)}</b>
      </div>
      ${if (!pago.observacion.isNullOrBlank()) "<p style='color:#777;margin-top:10px'>Obs: ${pago.observacion}</p>" else ""}
    </body></html>
    """.trimIndent()

        val jobName = "Pago_${compra.codigo}_${pago.idPagoCompra}"
        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String?) {
                val pm = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                val adapter = view.createPrintDocumentAdapter(jobName)
                pm.print(jobName, adapter, PrintAttributes.Builder().build())
            }
        }
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }

    fun imprimirComprobanteCompra(
        context: Context,
        compra: Compra,
        detalle: List<DetalleCompra>,
        pagos: List<PagoCompra>,
        pagoDestacado: PagoCompra? = null   // <-- nuevo
    )
    {
        val base = compra.total / 1.13
        val iva = base * 0.13
        val it = base * 0.03

        val filasProductos = detalle.joinToString("") { d -> /* igual que antes */
            """<tr>
             <td>${d.nombreProducto ?: "-"}</td>
             <td>${d.cantidad} unidad</td>
             <td>${bs(d.precioUnitario)}</td>
             <td style="font-weight:bold">${bs(d.subtotal)}</td>
           </tr>"""
        }

        val filasPagos = if (pagos.isEmpty()) {
            "<tr><td colspan=4 style='color:#999'>Sin pagos registrados</td></tr>"
        } else {
            pagos.joinToString("") { p ->
                val esDestacado = pagoDestacado != null && p.idPagoCompra == pagoDestacado.idPagoCompra
                val estilo = if (esDestacado) "background:#fff3e0;font-weight:bold" else ""
                """<tr style="$estilo">
                 <td>${p.fecha}</td>
                 <td>${p.metodoPago}</td>
                 <td>${bs(p.monto)}</td>
                 <td>${p.registradoPor ?: "-"}</td>
               </tr>"""
            }
        }

        // Franja superior: solo aparece si viene de registrar un abono
        val cintaAbono = if (pagoDestacado != null) {
            """<div style="background:#111;color:#fff;padding:8px 14px;border-radius:6px;margin-bottom:14px;display:flex;justify-content:space-between">
             <span>COMPROBANTE DE PAGO · Abono N.° ${pagos.indexOfFirst { it.idPagoCompra == pagoDestacado.idPagoCompra } + 1}</span>
             <span>${bs(pagoDestacado.monto)}</span>
           </div>"""
        } else ""

        val saldo = compra.saldoPendiente
        val estadoSaldo = if (saldo <= 0.009)
            """<span style="color:#2e7d32;font-weight:bold">Compra saldada</span>"""
        else
            """<span style="color:#c62828;font-weight:bold">${bs(saldo)}</span>"""

        val html = """
    <html><body style="font-family:sans-serif;padding:28px;color:#222">
      $cintaAbono
      <div style="display:flex;justify-content:space-between;border-bottom:2px solid #ddd;padding-bottom:14px">
        <div>
          <h2 style="margin:0">$NOMBRE_NEGOCIO</h2>
          <p style="margin:2px 0;color:#555">$DIRECCION<br/>NIT: $NIT</p>
        </div>
        <div style="text-align:right">
          <div style="border:1px solid #333;border-radius:6px;padding:8px 14px">
            <b>COMPROBANTE DE COMPRA</b><br/>${compra.codigo}
          </div>
        </div>
      </div>

      <table width="100%" style="margin-top:18px">
        <tr>
          <td><b>Fecha</b><br/>${compra.fechaEmision}</td>
          <td><b>Proveedor</b><br/>${compra.razonSocialProveedor ?: "-"}<br/>NIT: ${compra.rfcNitProveedor ?: "-"}</td>
          <td><b>Registrado por</b><br/>${if (com.example.scarlet.util.Session.estaLogueado) com.example.scarlet.util.Session.usuario else "admin"}</td>
        </tr>
      </table>

      <table width="100%" cellpadding="8" style="border-collapse:collapse;margin-top:18px">
        <tr style="border-bottom:2px solid #000;text-align:left">
          <th>Producto</th><th>Cant.</th><th>P. Unit.</th><th>Subtotal</th>
        </tr>
        $filasProductos
      </table>

      <table width="100%" style="margin-top:10px">
        <tr><td align="right" width="80%">Importe base (s/IVA)</td><td align="right">${bs(base)}</td></tr>
        <tr><td align="right">IVA (13%)</td><td align="right">${bs(iva)}</td></tr>
        <tr><td align="right">IT (3%)</td><td align="right">${bs(it)}</td></tr>
      </table>
      <div style="background:#111;color:#fff;padding:10px 16px;display:flex;justify-content:space-between;margin-top:8px">
        <b>TOTAL</b><b>${bs(compra.total)}</b>
      </div>

      <h3 style="margin-top:26px">Pago al proveedor</h3>
      <p>Pagado: <b>${bs(compra.totalPagado)}</b> / ${bs(compra.total)}<br/>
         Saldo pendiente: $estadoSaldo</p>

      <table width="100%" cellpadding="8" style="border-collapse:collapse">
        <tr style="border-bottom:2px solid #000;text-align:left">
          <th>Fecha</th><th>Método</th><th>Monto</th><th>Registrado por</th>
        </tr>
        $filasPagos
      </table>
    </body></html>
    """.trimIndent()

        val jobName = if (pagoDestacado != null) "Pago_${compra.codigo}" else "Comprobante_${compra.codigo}"
        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String?) {
                val pm = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                val adapter = view.createPrintDocumentAdapter(jobName)
                pm.print(jobName, adapter, PrintAttributes.Builder().build())
            }
        }
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }
    fun imprimirReporte(
        context: Context,
        periodo: String,
        desde: String,
        hasta: String,
        alcance: String,
        totalVentas: Double,
        subtotalVentas: Double,
        iva: Double,
        it: Double,
        ganancia: Double,
        cantidadVentas: Int,
        ticketPromedio: Double,
        rendimiento: List<Pair<String, Double>>,
        topProductos: List<TopProducto>
    ) {
        val filasRendimiento = if (rendimiento.isEmpty()) {
            "<tr><td colspan=2 style='color:#999'>Sin datos en el periodo</td></tr>"
        } else {
            rendimiento.joinToString("") { (etiqueta, monto) ->
                """<tr>
                 <td>$etiqueta</td>
                 <td align="right">${bs(monto)}</td>
               </tr>"""
            }
        }

        val filasTopProductos = if (topProductos.isEmpty()) {
            "<tr><td colspan=4 style='color:#999'>Sin ventas en el periodo</td></tr>"
        } else {
            topProductos.joinToString("") { p ->
                """<tr>
                 <td>${p.nombre}</td>
                 <td>${p.categoria}</td>
                 <td>${p.vendidos} unidad${if (p.vendidos == 1) "" else "es"}</td>
                 <td style="font-weight:bold">${bs(p.precio)}</td>
               </tr>"""
            }
        }

        val html = """
    <html><body style="font-family:sans-serif;padding:28px;color:#222">
      <div style="display:flex;justify-content:space-between;border-bottom:2px solid #ddd;padding-bottom:14px">
        <div>
          <h2 style="margin:0">$NOMBRE_NEGOCIO</h2>
          <p style="margin:2px 0;color:#555">$DIRECCION<br/>NIT: $NIT</p>
        </div>
        <div style="text-align:right">
          <div style="border:1px solid #333;border-radius:6px;padding:8px 14px">
            <b>REPORTE ADMINISTRATIVO</b><br/>Periodo: $periodo
          </div>
        </div>
      </div>

      <table width="100%" style="margin-top:18px">
        <tr>
          <td><b>Rango</b><br/>$desde &nbsp;al&nbsp; $hasta</td>
          <td><b>Alcance</b><br/>$alcance</td>
        </tr>
      </table>

      <h3 style="margin-top:26px">Ventas</h3>
      <table width="100%" style="margin-top:6px">
        <tr><td align="right" width="70%">Subtotal (s/IVA)</td><td align="right">${bs(subtotalVentas)}</td></tr>
        <tr><td align="right">IVA (13%)</td><td align="right">${bs(iva)}</td></tr>
        <tr><td align="right">IT (3%)</td><td align="right">${bs(it)}</td></tr>
        <tr><td align="right">Ganancia estimada</td><td align="right">${bs(ganancia)}</td></tr>
        <tr><td align="right">Cantidad de ventas</td><td align="right">$cantidadVentas</td></tr>
        <tr><td align="right">Ticket promedio</td><td align="right">${bs(ticketPromedio)}</td></tr>
      </table>
      <div style="background:#111;color:#fff;padding:10px 16px;display:flex;justify-content:space-between;margin-top:8px">
        <b>TOTAL VENDIDO</b><b>${bs(totalVentas)}</b>
      </div>

      <h3 style="margin-top:26px">Rendimiento</h3>
      <table width="100%" cellpadding="6" style="border-collapse:collapse">
        <tr style="border-bottom:2px solid #000;text-align:left"><th>Tramo</th><th align="right">Total</th></tr>
        $filasRendimiento
      </table>

      <h3 style="margin-top:26px">Productos más vendidos</h3>
      <table width="100%" cellpadding="8" style="border-collapse:collapse">
        <tr style="border-bottom:2px solid #000;text-align:left">
          <th>Producto</th><th>Categoría</th><th>Unidades</th><th>Precio</th>
        </tr>
        $filasTopProductos
      </table>
    </body></html>
    """.trimIndent()

        val jobName = "Reporte_${periodo}_${desde}_$hasta"
        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String?) {
                val pm = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                val adapter = view.createPrintDocumentAdapter(jobName)
                pm.print(jobName, adapter, PrintAttributes.Builder().build())
            }
        }
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }
    fun imprimirVenta(
        context: Context,
        idVenta: Int,
        fecha: String,
        cajero: String,
        cliente: String,
        metodoPago: String,
        items: List<Triple<String, Int, Double>>,
        subtotal: Double,
        iva: Double,
        impuestoIt: Double,
        total: Double
    ) {
        val filasProductos = items.joinToString("") { (nombre, cantidad, subtotalLinea) ->
            val precioUnit = if (cantidad > 0) subtotalLinea / cantidad else subtotalLinea
            """<tr>
             <td>$nombre</td>
             <td>$cantidad</td>
             <td>${bs(precioUnit)}</td>
             <td style="font-weight:bold">${bs(subtotalLinea)}</td>
           </tr>"""
        }

        val html = """
    <html><body style="font-family:sans-serif;padding:28px;color:#222">
      <div style="display:flex;justify-content:space-between;border-bottom:2px solid #ddd;padding-bottom:14px">
        <div>
          <h2 style="margin:0">$NOMBRE_NEGOCIO</h2>
          <p style="margin:2px 0;color:#555">$DIRECCION<br/>NIT: $NIT</p>
        </div>
        <div style="text-align:right">
          <div style="border:1px solid #333;border-radius:6px;padding:8px 14px">
            <b>COMPROBANTE DE VENTA</b><br/>Venta #$idVenta
          </div>
        </div>
      </div>

      <table width="100%" style="margin-top:18px">
        <tr>
          <td><b>Fecha</b><br/>$fecha</td>
          <td><b>Cliente</b><br/>$cliente</td>
          <td><b>Atendido por</b><br/>$cajero</td>
        </tr>
      </table>

      <table width="100%" cellpadding="8" style="border-collapse:collapse;margin-top:18px">
        <tr style="border-bottom:2px solid #000;text-align:left">
          <th>Producto</th><th>Cant.</th><th>P. Unit.</th><th>Subtotal</th>
        </tr>
        $filasProductos
      </table>

      <table width="100%" style="margin-top:10px">
        <tr><td align="right" width="80%">Subtotal (s/IVA)</td><td align="right">${bs(subtotal)}</td></tr>
        <tr><td align="right">IVA (13%)</td><td align="right">${bs(iva)}</td></tr>
        <tr><td align="right">IT (3%)</td><td align="right">${bs(impuestoIt)}</td></tr>
        <tr><td align="right">Método de pago</td><td align="right">$metodoPago</td></tr>
      </table>
      <div style="background:#111;color:#fff;padding:10px 16px;display:flex;justify-content:space-between;margin-top:8px">
        <b>TOTAL</b><b>${bs(total)}</b>
      </div>
    </body></html>
    """.trimIndent()

        val jobName = "Venta_$idVenta"
        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String?) {
                val pm = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                val adapter = view.createPrintDocumentAdapter(jobName)
                pm.print(jobName, adapter, PrintAttributes.Builder().build())
            }
        }
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }
}