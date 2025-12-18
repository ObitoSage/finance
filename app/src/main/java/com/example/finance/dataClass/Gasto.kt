package com.example.finance.dataClass

import java.util.Date

//Modelo de datos para representar un gasto
data class Gasto(
    val id: String = "",
    val categoria: String = "",
    val descripcion: String = "",
    val monto: Double = 0.0,
    val fecha: Date = Date(),
    val userId: String = ""
)
