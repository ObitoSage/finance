package com.example.finance.dataClass

import java.util.Date

// Modelo de datos para representar una transacción (gasto o ingreso) en el historial
data class Transaccion(
    val id: String = "",
    val tipo: String = "", // "gasto" o "ingreso"
    val categoria: String = "",
    val descripcion: String = "",
    val monto: Double = 0.0,
    val fecha: Date = Date()
)
