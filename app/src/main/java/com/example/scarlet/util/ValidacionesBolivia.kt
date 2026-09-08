package com.example.scarlet.util

/**
 * Reglas de formato para documentos y contactos bolivianos, usadas en todos
 * los formularios de la app que piden CI, NIT o celular (Mi cuenta,
 * Gestión de Cajeros y Proveedores).
 *
 * - CI: solo números, de 6 a 8 dígitos.
 * - NIT: solo números, de 10 a 12 dígitos.
 * - Celular: solo números, exactamente 8 dígitos, empieza con 6 o 7.
 */
object ValidacionesBolivia {

    private val REGEX_CI = Regex("^\\d{6,8}$")
    private val REGEX_NIT = Regex("^\\d{10,12}$")
    private val REGEX_CELULAR = Regex("^[67]\\d{7}$")

    const val MENSAJE_CI = "El CI debe tener solo números, de 6 a 8 dígitos"
    const val MENSAJE_NIT = "El NIT debe tener solo números, de 10 a 12 dígitos"
    const val MENSAJE_CELULAR = "El celular debe tener 8 dígitos y empezar con 6 o 7"

    fun esCiValido(ci: String): Boolean = REGEX_CI.matches(ci)

    fun esNitValido(nit: String): Boolean = REGEX_NIT.matches(nit)

    fun esCelularValido(celular: String): Boolean = REGEX_CELULAR.matches(celular)
}