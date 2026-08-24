package com.example.scarlet.data.model

data class Cuenta(
    val id_cuenta: Int = 0,
    val usuario: String,
    val clave: String,
    val estado: String,
    val id_persona: Int,
    val id_rol: Int
)
