package com.example.scarlet.util

import android.content.Context
import android.content.SharedPreferences

/**
 * Guarda en memoria (y ahora también en SharedPreferences) los datos de la
 * cuenta que inició sesión. Se establece en Login/MiCuenta y se consulta
 * desde el resto de las pantallas (por ejemplo, para saber qué cuenta
 * registró una venta, o si puede entrar a una pantalla de administrador).
 *
 * IMPORTANTE: antes esta sesión vivía solo en memoria (un objeto Kotlin
 * normal). Eso significa que si Android mataba el proceso de la app en
 * segundo plano —algo común mientras está abierto el diálogo del sistema
 * para imprimir/guardar un PDF de un comprobante— al volver, la app se
 * reconstruía con la sesión vacía y expulsaba al usuario de pantallas de
 * administrador aunque hubiera iniciado sesión como Administrador.
 *
 * Ahora Session también persiste sus datos en un SharedPreferences y se
 * restaura sola la primera vez que algo la consulta después de que el
 * proceso vuelve a arrancar, siempre que [inicializar] se haya llamado
 * (ver ScarletApp.onCreate()).
 */
object Session {

    private const val PREFS_NOMBRE = "scarlet_session"
    private const val KEY_ID_CUENTA = "id_cuenta"
    private const val KEY_USUARIO = "usuario"
    private const val KEY_NOMBRE_COMPLETO = "nombre_completo"
    private const val KEY_ROL = "rol"

    private var appContext: Context? = null
    private var restaurado = false

    private var _idCuenta: Int = -1
    private var _usuario: String = ""
    private var _nombreCompleto: String = ""
    private var _rol: String = ""

    val idCuenta: Int
        get() { restaurarSiHaceFalta(); return _idCuenta }

    val usuario: String
        get() { restaurarSiHaceFalta(); return _usuario }

    val nombreCompleto: String
        get() { restaurarSiHaceFalta(); return _nombreCompleto }

    val rol: String
        get() { restaurarSiHaceFalta(); return _rol }

    val estaLogueado: Boolean
        get() = idCuenta > 0

    // El nombre exacto del rol en la tabla `roles` es "Administrador" o "Vendedor"
    // (este último se usa como "Cajero" de cara al usuario).
    val esAdmin: Boolean
        get() = rol.equals("Administrador", ignoreCase = true)

    val esCajero: Boolean
        get() = !esAdmin

    /**
     * Debe llamarse una sola vez, apenas arranca el proceso (ScarletApp.onCreate).
     * Le da a Session el Context de aplicación que necesita para leer/escribir
     * SharedPreferences. Es seguro llamarla más de una vez.
     */
    fun inicializar(context: Context) {
        if (appContext == null) {
            appContext = context.applicationContext
        }
    }

    fun iniciar(idCuenta: Int, usuario: String, nombreCompleto: String, rol: String) {
        _idCuenta = idCuenta
        _usuario = usuario
        _nombreCompleto = nombreCompleto
        _rol = rol
        restaurado = true
        guardarEnDisco()
    }

    fun cerrar() {
        _idCuenta = -1
        _usuario = ""
        _nombreCompleto = ""
        _rol = ""
        restaurado = true
        prefs()?.edit()?.clear()?.apply()
    }

    private fun prefs(): SharedPreferences? =
        appContext?.getSharedPreferences(PREFS_NOMBRE, Context.MODE_PRIVATE)

    private fun guardarEnDisco() {
        prefs()?.edit()
            ?.putInt(KEY_ID_CUENTA, _idCuenta)
            ?.putString(KEY_USUARIO, _usuario)
            ?.putString(KEY_NOMBRE_COMPLETO, _nombreCompleto)
            ?.putString(KEY_ROL, _rol)
            ?.apply()
    }

    /**
     * Si todavía no restauramos la sesión en este proceso (por ejemplo,
     * porque Android acaba de recrear la app después de matarla en segundo
     * plano), la leemos de SharedPreferences. Si nunca hubo sesión guardada,
     * simplemente queda como "no logueado" (comportamiento igual al de antes).
     */
    private fun restaurarSiHaceFalta() {
        if (restaurado) return
        restaurado = true

        val prefs = prefs() ?: return
        _idCuenta = prefs.getInt(KEY_ID_CUENTA, -1)
        _usuario = prefs.getString(KEY_USUARIO, "") ?: ""
        _nombreCompleto = prefs.getString(KEY_NOMBRE_COMPLETO, "") ?: ""
        _rol = prefs.getString(KEY_ROL, "") ?: ""
    }
}