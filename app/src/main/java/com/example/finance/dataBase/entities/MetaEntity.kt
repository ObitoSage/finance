package com.example.finance.dataBase.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "metas")
data class MetaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nombre: String,
    val icono: String, // "target", "plane", "piggybank", etc.
    val ahorrado: Double,
    val objetivo: Double,
    val color: String, // Hex color
    val fechaLimite: Long?, // timestamp en milisegundos (opcional)
    val userId: String // ID del usuario de Firebase Auth
)
