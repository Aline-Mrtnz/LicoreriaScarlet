package com.example.scarlet.data.model

data class Categorias(
    val id_categoria: Int = 0,
    val nombre_categoria: String,
    val descripcion: String,
    // Ruta absoluta de la imagen subida por el usuario, o el nombre de un
    // drawable existente en el proyecto (categorías de ejemplo).
    val imagen_referencia: String? = null,
    // Etiquetas o marcas clave separadas por coma, ej: "Single Malt, Blended, Bourbon"
    val etiquetas: String? = null,
    // Si se destaca en el inicio de la app (carruseles / accesos directos)
    val destacado: Boolean = false,
    // Orden de aparición en menús y listados
    val orden_menu: Int = 0,
    // "ACTIVO" o "INACTIVO"
    val estado: String = "ACTIVO"
) {
    val estaActiva: Boolean
        get() = estado.equals("ACTIVO", ignoreCase = true)

    /** Lista de etiquetas ya separadas y sin espacios sobrantes. */
    fun listaEtiquetas(): List<String> =
        etiquetas
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?: emptyList()
}