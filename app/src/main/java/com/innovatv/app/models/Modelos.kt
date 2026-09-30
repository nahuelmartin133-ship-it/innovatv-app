package com.innovatv.app.models

data class Usuario(
    val usuario: String,
    val password: String,
    val servidor: String
)

data class Categoria(
    val id: String,
    val nombre: String
)

data class Canal(
    val num: Int,
    val nombre: String,
    val categoria: String,
    val categoriaId: String,
    val logo: String,
    val streamId: Int,
    val epgActual: String = ""
)

data class Pelicula(
    val id: Int,
    val titulo: String,
    val caratula: String,
    val categoria: String,
    val anio: String,
    val extension: String
)

data class ProgramitaEPG(
    val titulo: String,
    val descripcion: String,
    val inicio: String,
    val fin: String
)