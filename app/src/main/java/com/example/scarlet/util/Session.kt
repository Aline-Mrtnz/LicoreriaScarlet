package com.example.scarlet.util

/**
 * Guarda en memoria los datos de la cuenta que inició sesión.
 * Se establece en Login y se consulta desde el resto de las pantallas
 * (por ejemplo, para saber qué cuenta registró una venta).
 */
object Session {
    var idCuenta: Int = -1
        private set
    var usuario: String = ""
        private set
    var nombreCompleto: String = ""
        private set
    var rol: String = ""
        private set

    val estaLogueado: Boolean
        get() = idCuenta > 0

    fun iniciar(idCuenta: Int, usuario: String, nombreCompleto: String, rol: String) {
        this.idCuenta = idCuenta
        this.usuario = usuario
        this.nombreCompleto = nombreCompleto
        this.rol = rol
    }

    fun cerrar() {
        idCuenta = -1
        usuario = ""
        nombreCompleto = ""
        rol = ""
    }
}
