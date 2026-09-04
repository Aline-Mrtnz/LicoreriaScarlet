// app/src/main/java/com/example/scarlet/data/model/Proveedor.kt

package com.example.scarlet.data.model

data class Proveedor(
    val idProveedor: Int = 0,
    val razonSocial: String,
    val rfcNit: String,
    val condicionPago: String,
    val marcasAsociadas: String,
    val contactoEjecutivo: String,
    val telefonoContacto: String,
    val estado: String = "ACTIVO",
    // Campo calculado (no vive en la tabla proveedores): cuántos productos
    // tiene vinculados este proveedor en proveedor_productos.
    val cantidadProductos: Int = 0
) {
    val esActivo: Boolean
        get() = estado.equals("ACTIVO", ignoreCase = true)

    // Lista de marcas separadas, lista para pintar como texto/chips.
    val listaMarcas: List<String>
        get() = marcasAsociadas
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    // Iniciales para el avatar circular (ej. "Distribuidor Copacabana" -> "DC")
    val iniciales: String
        get() {
            val palabras = razonSocial.trim().split(" ").filter { it.isNotBlank() }
            return when {
                palabras.isEmpty() -> "?"
                palabras.size == 1 -> palabras[0].take(2).uppercase()
                else -> (palabras[0].take(1) + palabras[1].take(1)).uppercase()
            }
        }
}