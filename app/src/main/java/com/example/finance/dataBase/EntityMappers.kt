package com.example.finance.dataBase

import com.example.finance.dataBase.entities.GastoEntity
import com.example.finance.dataBase.entities.IngresoEntity
import com.example.finance.dataClass.Gasto
import com.example.finance.dataClass.Ingreso
import java.util.Date

//Esto mantiene separada la lógica de presentación de la lógica de persistencia.

object EntityMappers {
    
    //GASTOS
    fun Gasto.toEntity(): GastoEntity {
        return GastoEntity(
            id = if (this.id.isNotEmpty()) this.id.toLongOrNull() ?: 0 else 0,
            categoria = this.categoria,
            descripcion = this.descripcion,
            monto = this.monto,
            fecha = this.fecha.time,
            userId = this.userId
        )
    }

    fun GastoEntity.toDomain(): Gasto {
        return Gasto(
            id = this.id.toString(),
            categoria = this.categoria,
            descripcion = this.descripcion,
            monto = this.monto,
            fecha = Date(this.fecha),
            userId = this.userId
        )
    }

    fun List<GastoEntity>.toGastosDomain(): List<Gasto> {
        return this.map { it.toDomain() }
    }
    
    // INGRESOS
    fun Ingreso.toEntity(): IngresoEntity {
        return IngresoEntity(
            id = if (this.id.isNotEmpty()) this.id.toLongOrNull() ?: 0 else 0,
            categoria = this.categoria,
            descripcion = this.descripcion,
            monto = this.monto,
            fecha = this.fecha.time,
            userId = this.userId
        )
    }
    
    fun IngresoEntity.toDomain(): Ingreso {
        return Ingreso(
            id = this.id.toString(),
            categoria = this.categoria,
            descripcion = this.descripcion,
            monto = this.monto,
            fecha = Date(this.fecha),
            userId = this.userId
        )
    }

    fun List<IngresoEntity>.toIngresosDomain(): List<Ingreso> {
        return this.map { it.toDomain() }
    }
}
