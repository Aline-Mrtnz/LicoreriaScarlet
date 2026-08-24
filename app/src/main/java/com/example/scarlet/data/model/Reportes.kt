package com.example.scarlet.data.model

data class Reportes(
    val id_reporte: Int = 0,
    val tipo_reporte: String,
    val descripcion: String,
    val fecha_generacion: String,
    val cuenta_id_cuenta: Int
)
