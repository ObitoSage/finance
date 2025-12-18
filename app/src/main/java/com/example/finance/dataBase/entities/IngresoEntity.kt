package com.example.finance.dataBase.entities

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "ingresos")
data class IngresoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val categoria: String,
    val descripcion: String,
    val monto: Double,
    val fecha: Long, // Timestamp en milisegundos
    val userId: String // ID del usuario de Firebase Auth
)
