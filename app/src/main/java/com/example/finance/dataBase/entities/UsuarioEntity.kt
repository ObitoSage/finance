package com.example.finance.dataBase.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "usuarios")
data class UsuarioEntity(
    @PrimaryKey
    val userId: String, // ID del usuario de Firebase Auth
    val presupuestoMensual: Double = 0.0,
    val fechaActualizacion: Long = System.currentTimeMillis() // Timestamp en milisegundos
)
