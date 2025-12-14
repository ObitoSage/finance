package com.example.finance.dataClass

data class Meta(
    val id: String,
    val nombre: String,
    val icono: String, // "target", "plane", "piggybank", "home", "car", etc.
    val ahorrado: Double,
    val objetivo: Double,
    val color: String, // Hex color
    val fechaLimite: Long? = null // timestamp en milisegundos
) {
    fun getPorcentaje(): Float {
        if (objetivo == 0.0) return 0f
        return ((ahorrado / objetivo) * 100).toFloat()
    }
    
    fun getFaltante(): Double {
        return objetivo - ahorrado
    }
}
