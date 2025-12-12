package com.example.finance.Dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.finance.dataBase.entities.IngresoEntity
import kotlinx.coroutines.flow.Flow


@Dao
interface IngresoDao {
        /**
     * Inserta un nuevo ingreso en la base de datos.
     * @return El ID del ingreso insertado.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIngreso(ingreso: IngresoEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIngresos(ingresos: List<IngresoEntity>)
    
    //PATCH
    
    /**
     * Actualiza un ingreso existente.
     */
    @Update
    suspend fun updateIngreso(ingreso: IngresoEntity)
    
    //DELETE
    
    /**
     * Elimina un ingreso específico.
     */
    @Delete
    suspend fun deleteIngreso(ingreso: IngresoEntity)
    
    /**
     * Elimina un ingreso por su ID.
     */
    @Query("DELETE FROM ingresos WHERE id = :ingresoId")
    suspend fun deleteIngresoById(ingresoId: Long)

    @Query("DELETE FROM ingresos WHERE userId = :userId")
    suspend fun deleteAllIngresosByUser(userId: String)

    @Query("DELETE FROM ingresos")
    suspend fun deleteAllIngresos()
    
    //GET
    
    /**
     * Obtiene todos los ingresos de un usuario ordenados por fecha descendente.
     * Retorna un Flow para observar cambios en tiempo real.
     */
    @Query("SELECT * FROM ingresos WHERE userId = :userId ORDER BY fecha DESC")
    fun getAllIngresosByUser(userId: String): Flow<List<IngresoEntity>>
    
    /**
     * Obtiene todos los ingresos de un usuario (lista simple, sin Flow).
     */
    @Query("SELECT * FROM ingresos WHERE userId = :userId ORDER BY fecha DESC")
    suspend fun getAllIngresosByUserList(userId: String): List<IngresoEntity>
    
    /**
     * Obtiene un ingreso específico por su ID.
     */
    @Query("SELECT * FROM ingresos WHERE id = :ingresoId")
    suspend fun getIngresoById(ingresoId: Long): IngresoEntity?
    
    /**
     * Obtiene ingresos por categoría para un usuario específico.
     */
    @Query("SELECT * FROM ingresos WHERE userId = :userId AND categoria = :categoria ORDER BY fecha DESC")
    fun getIngresosByCategoria(userId: String, categoria: String): Flow<List<IngresoEntity>>
    
    /**
     * Obtiene ingresos en un rango de fechas para un usuario.
     */
    @Query("SELECT * FROM ingresos WHERE userId = :userId AND fecha BETWEEN :fechaInicio AND :fechaFin ORDER BY fecha DESC")
    fun getIngresosByDateRange(userId: String, fechaInicio: Long, fechaFin: Long): Flow<List<IngresoEntity>>
    
    /**
     * Obtiene el total de ingresos de un usuario.
     */
    @Query("SELECT SUM(monto) FROM ingresos WHERE userId = :userId")
    suspend fun getTotalIngresos(userId: String): Double?
    
    /**
     * Obtiene el total de ingresos por categoría para un usuario.
     */
    @Query("SELECT SUM(monto) FROM ingresos WHERE userId = :userId AND categoria = :categoria")
    suspend fun getTotalIngresosByCategoria(userId: String, categoria: String): Double?
    
    /**
     * Obtiene el total de ingresos en un rango de fechas.
     */
    @Query("SELECT SUM(monto) FROM ingresos WHERE userId = :userId AND fecha BETWEEN :fechaInicio AND :fechaFin")
    suspend fun getTotalIngresosByDateRange(userId: String, fechaInicio: Long, fechaFin: Long): Double?
    
    /**
     * Cuenta el número total de ingresos de un usuario.
     */
    @Query("SELECT COUNT(*) FROM ingresos WHERE userId = :userId")
    suspend fun getIngresosCount(userId: String): Int
}
