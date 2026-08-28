package com.example.scarlet.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Todas las fechas de ventas se guardan con este mismo formato para poder
 * filtrarlas y ordenarlas como texto (orden lexicográfico == orden
 * cronológico).
 */
object FechaUtils {

    private const val PATRON = "yyyy-MM-dd HH:mm:ss"
    private const val PATRON_DIA = "yyyy-MM-dd"

    private fun formatoCompleto() = SimpleDateFormat(PATRON, Locale.getDefault())
    private fun formatoDia() = SimpleDateFormat(PATRON_DIA, Locale.getDefault())

    fun ahora(): String = formatoCompleto().format(Date())

    fun soloFecha(fechaCompleta: String): String {
        return try {
            fechaCompleta.substring(0, 10)
        } catch (e: Exception) {
            fechaCompleta
        }
    }

    private fun inicioDeHoy(): Calendar {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal
    }

    /** Devuelve el rango [desde, hasta] en el formato de la BD para el filtro dado. */
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
            SimpleDateFormat("EEE", Locale("es", "ES")).format(fecha)
                .replaceFirstChar { it.uppercase() }
        } catch (e: Exception) {
            fechaDia
        }
    }
}
