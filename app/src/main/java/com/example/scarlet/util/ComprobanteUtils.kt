package com.example.scarlet.util

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import com.example.scarlet.data.model.Compra
import com.example.scarlet.data.model.DetalleCompra
import com.example.scarlet.data.model.PagoCompra
import com.example.scarlet.data.model.TopProducto
import java.util.Locale

/**
 * Generación e impresión de comprobantes (ventas, compras, pagos de compra
 * y reportes) usando datos reales de Licorería Scarlet.
 *
 * Todos los comprobantes comparten el mismo encabezado del negocio y se
 * imprimen/comparten como PDF a través del diálogo de impresión de Android
 * (WebView + PrintManager), que en la mayoría de dispositivos permite
 * guardar como PDF o enviar directo a una impresora.
 */
object ComprobanteUtils {

    private const val NOMBRE_NEGOCIO = "Licorería Scarlet"
    private const val DIRECCION = "Cochabamba, Bolivia"

    private fun bs(monto: Double): String =
        "Bs " + String.format(Locale("es", "BO"), "%,.2f", monto)

    private fun encabezadoHtml(titulo: String, numero: String): String = """
        <div style="text-align:center;border-bottom:2px solid #111;padding-bottom:10px;margin-bottom:14px">
            <h2 style="margin:0">$NOMBRE_NEGOCIO</h2>
            <p style="margin:2px 0;color:#444">$DIRECCION</p>
            <h3 style="margin:10px 0 0 0">$titulo</h3>
            <p style="margin:2px 0;color:#444">$numero</p>
        </div>
    """.trimIndent()

    private fun envolverHtml(cuerpo: String): String = """
        <html><body style="font-family:sans-serif;padding:24px;color:#111">
        $cuerpo
        </body></html>
    """.trimIndent()

    private fun imprimirHtml(context: Context, html: String, jobName: String) {
        val webView = WebView(context)
        webView.webViewClient = object : android.webkit.WebViewClient() {
            override fun onPageFinished(view: WebView, url: String?) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                val adapter = view.createPrintDocumentAdapter(jobName)
                printManager.print(jobName, adapter, PrintAttributes.Builder().build())
            }
        }
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }

    // =============================================
    // 1. COMPROBANTE DE VENTA (recibo tras cada venta)
    // =============================================
    fun imprimirVenta(
        context: Context,
        idVenta: Int,
        fecha: String,
        cajero: String,
        cliente: String,
        metodoPago: String,
        items: List<Triple<String, Int, Double>>,
        subtotal: Double,
        impuestos: Double,
        total: Double
    ) {
        val filas = items.joinToString("") { (nombre, cant, subtotalLinea) ->
            "<tr><td style='padding:4px 0'>$nombre</td><td style='text-align:center'>$cant</td><td style='text-align:right'>${bs(subtotalLinea)}</td></tr>"
        }
        val cuerpo = """
            ${encabezadoHtml("COMPROBANTE DE VENTA", "N.º $idVenta")}
            <p>
                <b>Fecha:</b> $fecha<br/>
                <b>Cliente:</b> $cliente<br/>
                <b>Atendido por:</b> $cajero<br/>
                <b>Método de pago:</b> $metodoPago
            </p>
            <table width="100%" cellpadding="4" style="border-collapse:collapse;margin-top:10px">
                <tr style="border-bottom:1px solid #111">
                    <th style="text-align:left">Producto</th><th>Cant.</th><th style="text-align:right">Subtotal</th>
                </tr>
                $filas
            </table>
            <div style="margin-top:14px;text-align:right">
                <p style="margin:2px 0">Subtotal: ${bs(subtotal)}</p>
                <p style="margin:2px 0">Impuestos (16%): ${bs(impuestos)}</p>
                <h2 style="margin:6px 0">TOTAL: ${bs(total)}</h2>
            </div>
            <p style="text-align:center;color:#777;margin-top:20px;font-size:12px">¡Gracias por su compra en $NOMBRE_NEGOCIO!</p>
        """.trimIndent()
        imprimirHtml(context, envolverHtml(cuerpo), "Venta_$idVenta")
    }

    // =============================================
    // 2. COMPROBANTE DE COMPRA (orden de compra a proveedor)
    // =============================================
    fun imprimirCompra(context: Context, compra: Compra, detalle: List<DetalleCompra>) {
        val filas = detalle.joinToString("") { item ->
            "<tr><td style='padding:4px 0'>${item.nombreProducto ?: "-"}</td><td style='text-align:center'>${item.cantidad}</td>" +
                    "<td style='text-align:right'>${bs(item.precioUnitario)}</td><td style='text-align:right'>${bs(item.subtotal)}</td></tr>"
        }
        val cuerpo = """
            ${encabezadoHtml("COMPROBANTE DE COMPRA", compra.codigo)}
            <p>
                <b>Fecha de emisión:</b> ${compra.fechaEmision}<br/>
                <b>Proveedor:</b> ${compra.razonSocialProveedor ?: "-"}<br/>
                <b>NIT/RFC proveedor:</b> ${compra.rfcNitProveedor ?: "-"}<br/>
                <b>Estado:</b> ${compra.estado}
            </p>
            <table width="100%" cellpadding="4" style="border-collapse:collapse;margin-top:10px">
                <tr style="border-bottom:1px solid #111">
                    <th style="text-align:left">Producto</th><th>Cant.</th><th style="text-align:right">P. Unit.</th><th style="text-align:right">Subtotal</th>
                </tr>
                $filas
            </table>
            <div style="margin-top:14px;text-align:right">
                <p style="margin:2px 0">Pagado: ${bs(compra.totalPagado)}</p>
                <p style="margin:2px 0">Saldo pendiente: ${bs(compra.saldoPendiente)}</p>
                <h2 style="margin:6px 0">TOTAL: ${bs(compra.total)}</h2>
            </div>
        """.trimIndent()
        imprimirHtml(context, envolverHtml(cuerpo), "Compra_${compra.codigo}")
    }

    // =============================================
    // 3. COMPROBANTE DE PAGO/ABONO DE COMPRA
    // =============================================
    fun imprimirPagoCompra(context: Context, compra: Compra, pago: PagoCompra) {
        val cuerpo = """
            ${encabezadoHtml("COMPROBANTE DE PAGO", "Abono N.º ${pago.idPagoCompra}")}
            <p>
                <b>Fecha:</b> ${pago.fecha}<br/>
                <b>Orden de compra:</b> ${compra.codigo}<br/>
                <b>Proveedor:</b> ${compra.razonSocialProveedor ?: "-"}<br/>
                <b>Método de pago:</b> ${pago.metodoPago}<br/>
                <b>Registrado por:</b> ${pago.registradoPor ?: "-"}
                ${if (!pago.observacion.isNullOrBlank()) "<br/><b>Observación:</b> ${pago.observacion}" else ""}
            </p>
            <div style="margin-top:14px;text-align:right">
                ${if (pago.efectivoRecibido != null) "<p style='margin:2px 0'>Efectivo recibido: ${bs(pago.efectivoRecibido)}</p>" else ""}
                <h2 style="margin:6px 0">MONTO PAGADO: ${bs(pago.monto)}</h2>
                <p style="margin:2px 0;color:#444">Saldo pendiente de la orden: ${bs(compra.saldoPendiente)}</p>
            </div>
        """.trimIndent()
        imprimirHtml(context, envolverHtml(cuerpo), "Pago_${compra.codigo}_${pago.idPagoCompra}")
    }

    // =============================================
    // 4. COMPROBANTE / RESUMEN DE REPORTE
    // =============================================
    fun imprimirReporte(
        context: Context,
        periodo: String,
        desde: String,
        hasta: String,
        totalVentas: Double,
        ganancia: Double,
        cantidadVentas: Int,
        ticketPromedio: Double,
        topProductos: List<TopProducto>
    ) {
        val filasTop = topProductos.mapIndexed { index, producto ->
            "<tr><td style='padding:4px 0'>${index + 1}. ${producto.nombre}</td><td style='text-align:center'>${producto.vendidos}</td>" +
                    "<td style='text-align:right'>${bs(producto.precio)}</td></tr>"
        }.joinToString("")

        val cuerpo = """
            ${encabezadoHtml("COMPROBANTE DE REPORTE", "Periodo: $periodo")}
            <p>
                <b>Desde:</b> $desde<br/>
                <b>Hasta:</b> $hasta<br/>
                <b>Generado:</b> ${FechaUtils.ahora()}
            </p>
            <table width="100%" cellpadding="6" style="border-collapse:collapse;margin-top:10px">
                <tr><td>Total ventas</td><td style="text-align:right"><b>${bs(totalVentas)}</b></td></tr>
                <tr><td>Ganancia</td><td style="text-align:right"><b>${bs(ganancia)}</b></td></tr>
                <tr><td>Cantidad de ventas</td><td style="text-align:right"><b>$cantidadVentas</b></td></tr>
                <tr><td>Ticket promedio</td><td style="text-align:right"><b>${bs(ticketPromedio)}</b></td></tr>
            </table>
            ${
            if (topProductos.isNotEmpty()) """
                <h3 style="margin-top:18px">Top productos del periodo</h3>
                <table width="100%" cellpadding="4" style="border-collapse:collapse">
                    <tr style="border-bottom:1px solid #111"><th style="text-align:left">Producto</th><th>Vendidos</th><th style="text-align:right">Precio</th></tr>
                    $filasTop
                </table>
                """.trimIndent() else ""
        }
        """.trimIndent()
        imprimirHtml(context, envolverHtml(cuerpo), "Reporte_${periodo}_${FechaUtils.ahora().replace(Regex("[^0-9]"), "")}")
    }
}