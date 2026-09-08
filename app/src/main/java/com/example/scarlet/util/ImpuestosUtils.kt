package com.example.scarlet.util

/**
 * Fuente única de verdad para los impuestos de una venta en Bolivia:
 *   - IVA: 13%
 *   - IT:  3%
 *   - Total de impuestos: 16% (IVA + IT)
 *
 * ANTES: cada pantalla (Shopping, ComprobanteUtils, dialog_recibo_venta,
 * Reportes) tenía su propio "0.16" hardcodeado y solo se calculaba UNA vez,
 * en el momento del checkout. Ese total con impuestos nunca se guardaba en
 * la base de datos (VentasRepository guardaba solo el subtotal), así que
 * el Historial de Ventas, los Reportes y cualquier reimpresión posterior
 * mostraban un total distinto (sin impuestos) al que el cliente realmente
 * pagó. Con esto centralizado:
 *   1. Se guarda en `ventas.total` el monto CON impuestos (ver
 *      VentasRepository.registrarVentaCompleta).
 *   2. Para reconstruir subtotal/IVA/IT de una venta ya guardada, se usa
 *      [desdeTotalConImpuestos], que revierte la fórmula.
 */
object ImpuestosUtils {

    const val TASA_IVA = 0.13
    const val TASA_IT = 0.03
    const val TASA_TOTAL = TASA_IVA + TASA_IT // 0.16

    data class DesgloseImpuestos(
        val subtotal: Double,
        val iva: Double,
        val it: Double,
        val total: Double
    ) {
        val impuestos: Double get() = iva + it
    }

    /**
     * Redondea a centavos (2 decimales). Se usa para que lo que se muestra
     * y se guarda siempre cuadre exactamente: antes cada valor (subtotal,
     * IVA, IT, total) se redondeaba por separado solo al mostrarlo en
     * pantalla/recibo, y la suma de las líneas podía quedar 1 centavo por
     * debajo o por encima del total (ej: 72.41 + 9.41 + 2.17 = 83.99 pero
     * el total mostraba 84.00).
     */
    private fun redondear(valor: Double): Double = Math.round(valor * 100.0) / 100.0

    /**
     * A partir de un subtotal (sin impuestos), calcula IVA, IT y total.
     * El subtotal y el IVA se redondean normalmente a centavos; el IT se
     * calcula como el resto exacto (total - subtotal - iva) para GARANTIZAR
     * que subtotal + IVA + IT sea siempre igual al total mostrado.
     */
    fun desdeSubtotal(subtotalCrudo: Double): DesgloseImpuestos {
        val subtotal = redondear(subtotalCrudo)
        val iva = redondear(subtotalCrudo * TASA_IVA)
        val total = redondear(subtotalCrudo * (1.0 + TASA_TOTAL))
        val it = redondear(total - subtotal - iva)
        return DesgloseImpuestos(subtotal, iva, it, total)
    }

    /**
     * A partir de un total YA CON impuestos (como el que se guarda en
     * `ventas.total`), reconstruye el subtotal, el IVA y el IT.
     * Útil para reimprimir/ver la factura de una venta pasada, donde solo
     * tenemos el total guardado.
     *
     * Igual que en [desdeSubtotal]: subtotal e IVA se redondean normalmente
     * y el IT se calcula como el resto exacto (total - subtotal - iva),
     * para que la suma de las tres líneas del recibo/reporte siempre
     * coincida con el total mostrado (sin la diferencia de 1 centavo que
     * salía antes).
     */
    fun desdeTotalConImpuestos(totalCrudo: Double): DesgloseImpuestos {
        val total = redondear(totalCrudo)
        val subtotalCrudo = totalCrudo / (1.0 + TASA_TOTAL)
        val subtotal = redondear(subtotalCrudo)
        val iva = redondear(subtotalCrudo * TASA_IVA)
        val it = redondear(total - subtotal - iva)
        return DesgloseImpuestos(subtotal, iva, it, total)
    }
}