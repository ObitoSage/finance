package com.example.finance.models

import java.util.Date

// Modelo de datos para representar un ingreso
data class Ingreso(
    val id: String = "",
    val categoria: String = "",
    val descripcion: String = "",
    val monto: Double = 0.0,
    val fecha: Date = Date(),
    val userId: String = ""
)
