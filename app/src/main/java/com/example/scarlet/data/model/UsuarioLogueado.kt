package com.example.scarlet.data.model

// Representa toda la información que se necesita tras iniciar sesión:
// datos de la cuenta + de la persona + el nombre de su rol.
data class UsuarioLogueado(
    val id_cuenta: Int,
    val usuario: String,
    val estado: String,
    val id_persona: Int,
    val nombres: String,
    val apellidos: String,
    val id_rol: Int,
    val nombre_rol: String
)
