package com.example.scarlet.data.model

data class Productos(
    val id_producto: Int = 0,
    val nombre_producto: String,
    val descripcion: String,
    val imagen: String,
    val precio_venta: Double,
    val precio_mayor: Double,
    val stock: Int,
    val estado: String,
    val id_categoria: Int,
    val marcas_id_marca: Int
)
