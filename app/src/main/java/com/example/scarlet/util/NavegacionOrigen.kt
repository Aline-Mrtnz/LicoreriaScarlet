package com.example.scarlet.util

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.example.scarlet.MainActivity

/**
 * Hace que las pantallas abiertas desde el menú hamburguesa (Caja,
 * Categorías, Inventario, Compras/Reabastecimiento, Gestión de Cajeros,
 * Proveedores, Mi Cuenta) siempre puedan volver directo a la ventana
 * PRINCIPAL del footer (Inicio, Ventas, Productos o Reportes) desde la
 * que se entró, sin importar cuántos módulos del menú se hayan abierto
 * uno tras otro.
 *
 * Funcionamiento:
 * 1. Las ventanas principales del footer llaman a [abrirModulo] cuando
 *    el usuario toca un ítem del menú hamburguesa: eso deja "marcado"
 *    el origen dentro del Intent.
 * 2. Las ventanas secundarias (las que reciben ese origen) llaman a
 *    [cambiarModulo] cuando el usuario toca OTRO ítem del menú: así el
 *    origen se sigue llevando de pantalla en pantalla y no se acumulan
 *    ventanas en la pila.
 * 3. El botón "‹ Volver" de toda ventana secundaria llama a
 *    [volverAOrigen], que regresa directo a la ventana del footer que
 *    quedó marcada como origen (o a MainActivity si por algún motivo no
 *    hay origen registrado).
 */
object NavegacionOrigen {

    const val EXTRA_ORIGEN = "extra_origen_footer"

    /** Llamar desde las ventanas PRINCIPALES del footer (Inicio, Ventas,
     * Productos, Reportes) al abrir un módulo del menú hamburguesa. */
    fun abrirModulo(origen: AppCompatActivity, destino: Class<*>) {
        val intent = Intent(origen, destino)
        intent.putExtra(EXTRA_ORIGEN, origen::class.java.name)
        origen.startActivity(intent)
    }

    /** Llamar desde las ventanas SECUNDARIAS (Caja, Categorías,
     * Inventario, Compras, Cajeros, Proveedores, Mi Cuenta) al cambiar a
     * otro módulo del menú hamburguesa. Conserva el origen y cierra la
     * pantalla actual para no acumular ventanas en la pila. */
    fun cambiarModulo(actual: AppCompatActivity, destino: Class<*>) {
        val origenNombre = actual.intent.getStringExtra(EXTRA_ORIGEN)
        val intent = Intent(actual, destino)
        if (origenNombre != null) intent.putExtra(EXTRA_ORIGEN, origenNombre)
        actual.startActivity(intent)
        actual.finish()
    }

    /** Llamar desde el botón "‹ Volver" de toda ventana secundaria:
     * regresa directo a la ventana principal del footer desde la que se
     * entró (Inicio, Ventas, Productos o Reportes). */
    fun volverAOrigen(actual: AppCompatActivity, porDefecto: Class<*> = MainActivity::class.java) {
        val origenNombre = actual.intent.getStringExtra(EXTRA_ORIGEN)
        val claseOrigen = try {
            if (origenNombre != null) Class.forName(origenNombre) else porDefecto
        } catch (e: ClassNotFoundException) {
            porDefecto
        }
        val intent = Intent(actual, claseOrigen)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        actual.startActivity(intent)
        actual.finish()
    }
}