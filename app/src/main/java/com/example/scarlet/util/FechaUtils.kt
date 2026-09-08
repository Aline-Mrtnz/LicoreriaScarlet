package com.example.scarlet.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Todas las fechas de ventas/compras/pagos se guardan con este mismo
 * formato para poder filtrarlas y ordenarlas como texto (orden
 * lexicográfico == orden cronológico).
 *
 * IMPORTANTE (horario boliviano): antes todo esto usaba el huso horario
 * POR DEFECTO DEL CELULAR (`Calendar.getInstance()` / `Locale.getDefault()`
 * sin `timeZone`). Si el dispositivo tenía otra zona horaria configurada
 * (o "usar hora de la red" con el celular fuera de Bolivia), las fechas
 * guardadas en ventas/compras y las que se veían en comprobantes e
 * historial NO correspondían a la hora real de Bolivia. Ahora todo pasa
 * por [ZONA_BOLIVIA] (America/La_Paz, UTC-4 fijo, sin horario de verano),
 * así que la fecha/hora que se guarda al momento de la venta/compra y la
 * que se imprime en cualquier comprobante es siempre la hora real de
 * Bolivia, sin importar cómo esté configurado el celular.
 */
object FechaUtils {

    private const val PATRON = "yyyy-MM-dd HH:mm:ss"
    private const val PATRON_DIA = "yyyy-MM-dd"

    val ZONA_BOLIVIA: TimeZone = TimeZone.getTimeZone("America/La_Paz")

    private fun formatoCompleto() =
        SimpleDateFormat(PATRON, Locale.getDefault()).apply { timeZone = ZONA_BOLIVIA }

    private fun formatoDia() =
        SimpleDateFormat(PATRON_DIA, Locale.getDefault()).apply { timeZone = ZONA_BOLIVIA }

    /** Instante actual (hora real de Bolivia), en el formato que se guarda en la BD. */
    fun ahora(): String = formatoCompleto().format(Date())

    /**
     * Fecha y hora ACTUAL (momento de imprimir), lista para mostrarse en un
     * comprobante como "Impreso el: 07/09/2026 15:32 (hora de Bolivia)".
     * Se usa en los 4 comprobantes para dejar registrado, en tiempo real,
     * cuándo se generó/imprimió el documento.
     */
    fun fechaHoraActualLegible(): String {
        val formato = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "BO"))
        formato.timeZone = ZONA_BOLIVIA
        return "${formato.format(Date())} (hora de Bolivia)"
    }

    /** Convierte una fecha guardada ("yyyy-MM-dd HH:mm:ss") a un texto legible "dd/MM/yyyy HH:mm". */
    fun formatoLegible(fechaCompleta: String): String {
        return try {
            val fecha = formatoCompleto().parse(fechaCompleta) ?: return fechaCompleta
            val salida = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "BO"))
            salida.timeZone = ZONA_BOLIVIA
            salida.format(fecha)
        } catch (e: Exception) {
            fechaCompleta
        }
    }

    fun soloFecha(fechaCompleta: String): String {
        return try {
            fechaCompleta.substring(0, 10)
        } catch (e: Exception) {
            fechaCompleta
        }
    }

    /** Calendario anclado a la hora real de Bolivia (no a la zona del dispositivo). */
    private fun calendarioBolivia(): Calendar = Calendar.getInstance(ZONA_BOLIVIA)

    private fun inicioDeHoy(): Calendar {
        val cal = calendarioBolivia()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal
    }

    /** Rango [desde, hasta] en el formato de la BD para el filtro dado. */
    fun rangoParaFiltro(filtro: String): Pair<String, String> {
        val hasta = formatoCompleto().format(Date())
        val cal = inicioDeHoy()

        when (filtro) {
            "Semana" -> cal.add(Calendar.DAY_OF_YEAR, -6)
            "Mes" -> cal.add(Calendar.DAY_OF_YEAR, -29)
            "Año" -> cal.add(Calendar.DAY_OF_YEAR, -364)
            else -> { /* "Día": desde el inicio de hoy */ }
        }

        val desde = formatoCompleto().format(cal.time)
        return desde to hasta
    }

    /**
     * Rango del periodo INMEDIATAMENTE ANTERIOR al del filtro dado, con la
     * misma duración (p. ej. para "Semana" son los 7 días previos a los 7
     * días actuales). Se usa para calcular la tendencia real (▲/▼ %) de
     * Total Ventas y Ganancia en Reportes, en vez de un porcentaje fijo
     * inventado en el XML.
     */
    fun rangoAnteriorParaFiltro(filtro: String): Pair<String, String> {
        val duracionDias = when (filtro) {
            "Semana" -> 7
            "Mes" -> 30
            "Año" -> 365
            else -> 1 // "Día"
        }
        // Se ancla al mismo "desde" que usa rangoParaFiltro() para que el
        // periodo anterior termine justo donde empieza el actual, sin
        // huecos ni superposiciones.
        val (desdeActualStr, _) = rangoParaFiltro(filtro)
        val desdeActual = formatoCompleto().parse(desdeActualStr) ?: Date()

        val calHasta = calendarioBolivia()
        calHasta.time = desdeActual
        calHasta.add(Calendar.SECOND, -1)
        val hasta = formatoCompleto().format(calHasta.time)

        val calDesde = calendarioBolivia()
        calDesde.time = desdeActual
        calDesde.add(Calendar.DAY_OF_YEAR, -duracionDias)
        val desde = formatoCompleto().format(calDesde.time)

        return desde to hasta
    }

    /** Últimos 7 días (incluyendo hoy) en formato yyyy-MM-dd, ordenados cronológicamente. */
    fun ultimosNDias(n: Int): List<String> {
        val lista = mutableListOf<String>()
        val cal = inicioDeHoy()
        cal.add(Calendar.DAY_OF_YEAR, -(n - 1))
        repeat(n) {
            lista.add(formatoDia().format(cal.time))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return lista
    }

    fun etiquetaDiaCorta(fechaDia: String): String {
        return try {
            val fecha = formatoDia().parse(fechaDia) ?: return fechaDia
            val formato = SimpleDateFormat("EEE", Locale("es", "ES"))
            formato.timeZone = ZONA_BOLIVIA
            formato.format(fecha).replaceFirstChar { it.uppercase() }
        } catch (e: Exception) {
            fechaDia
        }
    }

    /**
     * Genera los 7 "buckets" (etiqueta, desde, hasta) que arma el gráfico de
     * "Rendimiento" en Reportes, según el filtro seleccionado.
     *
     * ANTES: el gráfico llamaba siempre a `ultimosNDias(7)` sin importar el
     * filtro Día/Semana/Mes/Año elegido arriba, así que se veía exactamente
     * igual eligieras lo que eligieras. Ahora cada filtro arma su propia
     * ventana de tiempo, dividida en 7 tramos (para reusar las 7 barras que
     * ya existen en el layout):
     *
     *   - Día:    últimas 7 franjas de 24/7 ≈ 3.4 horas de HOY, etiqueta "HH:00"
     *   - Semana: los últimos 7 días, uno por barra (comportamiento anterior)
     *   - Mes:    los últimos 28 días agrupados de a 4, etiqueta "d/M"
     *   - Año:    los últimos 7 meses, uno por barra, etiqueta "MMM"
     */
    fun bucketsParaGrafico(filtro: String): List<Triple<String, String, String>> {
        return when (filtro) {
            "Día" -> bucketsPorHoras()
            "Mes" -> bucketsPorRangoDeDias(diasTotales = 28, diasPorBucket = 4)
            "Año" -> bucketsPorMeses(7)
            else -> bucketsPorDias(7) // "Semana" (y cualquier valor no reconocido)
        }
    }

    private fun bucketsPorDias(n: Int): List<Triple<String, String, String>> {
        val dias = ultimosNDias(n)
        return dias.map { dia ->
            val desde = "$dia 00:00:00"
            val hasta = "$dia 23:59:59"
            Triple(etiquetaDiaCorta(dia), desde, hasta)
        }
    }

    private fun bucketsPorRangoDeDias(diasTotales: Int, diasPorBucket: Int): List<Triple<String, String, String>> {
        val resultado = mutableListOf<Triple<String, String, String>>()
        val cantidadBuckets = diasTotales / diasPorBucket
        val cal = inicioDeHoy()
        cal.add(Calendar.DAY_OF_YEAR, -(diasTotales - 1))
        val etiquetaFormato = SimpleDateFormat("d/M", Locale("es", "ES"))
        etiquetaFormato.timeZone = ZONA_BOLIVIA
        repeat(cantidadBuckets) {
            val inicioBucket = cal.clone() as Calendar
            val etiqueta = etiquetaFormato.format(inicioBucket.time)
            val desde = formatoCompleto().format(inicioBucket.time)
            cal.add(Calendar.DAY_OF_YEAR, diasPorBucket)
            val finBucket = cal.clone() as Calendar
            finBucket.add(Calendar.SECOND, -1)
            val hasta = formatoCompleto().format(finBucket.time)
            resultado.add(Triple(etiqueta, desde, hasta))
        }
        return resultado
    }

    private fun bucketsPorMeses(n: Int): List<Triple<String, String, String>> {
        val resultado = mutableListOf<Triple<String, String, String>>()
        val cal = calendarioBolivia()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.add(Calendar.MONTH, -(n - 1))
        val etiquetaFormato = SimpleDateFormat("MMM", Locale("es", "ES"))
        etiquetaFormato.timeZone = ZONA_BOLIVIA
        repeat(n) {
            val inicioMes = cal.clone() as Calendar
            val etiqueta = etiquetaFormato.format(inicioMes.time).replaceFirstChar { it.uppercase() }
            val desde = formatoCompleto().format(inicioMes.time)
            val finMes = cal.clone() as Calendar
            finMes.add(Calendar.MONTH, 1)
            finMes.add(Calendar.SECOND, -1)
            val hasta = formatoCompleto().format(finMes.time)
            resultado.add(Triple(etiqueta, desde, hasta))
            cal.add(Calendar.MONTH, 1)
        }
        return resultado
    }

    private fun bucketsPorHoras(): List<Triple<String, String, String>> {
        val resultado = mutableListOf<Triple<String, String, String>>()
        val ahoraCal = calendarioBolivia()
        val horaActual = ahoraCal.get(Calendar.HOUR_OF_DAY)
        // Se reparten las horas transcurridas de HOY (mínimo 7) en 7 tramos.
        val horasTranscurridas = (horaActual + 1).coerceAtLeast(7)
        val horasPorBucket = (horasTranscurridas / 7.0)
        val inicio = inicioDeHoy()
        val etiquetaFormato = SimpleDateFormat("HH:00", Locale("es", "ES"))
        etiquetaFormato.timeZone = ZONA_BOLIVIA
        var acumulado = 0.0
        repeat(7) { indice ->
            val inicioBucket = inicio.clone() as Calendar
            inicioBucket.add(Calendar.MINUTE, (acumulado * 60).toInt())
            acumulado += horasPorBucket
            val finBucket = inicio.clone() as Calendar
            finBucket.add(Calendar.MINUTE, (acumulado * 60).toInt() - 1)
            val etiqueta = etiquetaFormato.format(inicioBucket.time)
            val desde = formatoCompleto().format(inicioBucket.time)
            val hasta = if (indice == 6) formatoCompleto().format(Date()) else formatoCompleto().format(finBucket.time)
            resultado.add(Triple(etiqueta, desde, hasta))
        }
        return resultado
    }
}