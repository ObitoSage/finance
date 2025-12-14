package com.example.finance.Dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.finance.dataBase.entities.GastoEntity
import kotlinx.coroutines.flow.Flow


@Dao
interface GastoDao {
    
    // PUT
    
    /**
     * Inserta un nuevo gasto en la base de datos.
     * @return El ID del gasto insertado.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGasto(gasto: GastoEntity): Long
    
    /**
     * Inserta múltiples gastos en la base de datos.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGastos(gastos: List<GastoEntity>)
    
    //PATCH
    
    /**
     * Actualiza un gasto existente.
     */
    @Update
    suspend fun updateGasto(gasto: GastoEntity)
    
    //DELETE
    
    /**
     * Elimina un gasto específico.
     */
    @Delete
    suspend fun deleteGasto(gasto: GastoEntity)
    
    /**
     * Elimina un gasto por su ID.
     */
    @Query("DELETE FROM gastos WHERE id = :gastoId")
    suspend fun deleteGastoById(gastoId: Long)
    
    /**
     * Elimina todos los gastos de un usuario específico.
     */
    @Query("DELETE FROM gastos WHERE userId = :userId")
    suspend fun deleteAllGastosByUser(userId: String)
    
    /**
     * Elimina todos los gastos de la tabla.
     */
    @Query("DELETE FROM gastos")
    suspend fun deleteAllGastos()
    
    //GET
    
    /**
     * Obtiene todos los gastos de un usuario ordenados por fecha descendente.
     * Retorna un Flow para observar cambios en tiempo real.
     */
    @Query("SELECT * FROM gastos WHERE userId = :userId ORDER BY fecha DESC")
    fun getAllGastosByUser(userId: String): Flow<List<GastoEntity>>
    
    /**
     * Obtiene todos los gastos de un usuario (lista simple, sin Flow).
     */
    @Query("SELECT * FROM gastos WHERE userId = :userId ORDER BY fecha DESC")
    suspend fun getAllGastosByUserList(userId: String): List<GastoEntity>
    
    /**
     * Obtiene un gasto específico por su ID.
     */
    @Query("SELECT * FROM gastos WHERE id = :gastoId")
    suspend fun getGastoById(gastoId: Long): GastoEntity?
    
    /**
     * Obtiene gastos por categoría para un usuario específico.
     */
    @Query("SELECT * FROM gastos WHERE userId = :userId AND categoria = :categoria ORDER BY fecha DESC")
    fun getGastosByCategoria(userId: String, categoria: String): Flow<List<GastoEntity>>
    
    /**
     * Obtiene gastos en un rango de fechas para un usuario.
     */
    @Query("SELECT * FROM gastos WHERE userId = :userId AND fecha BETWEEN :fechaInicio AND :fechaFin ORDER BY fecha DESC")
    fun getGastosByDateRange(userId: String, fechaInicio: Long, fechaFin: Long): Flow<List<GastoEntity>>
    
    /**
     * Obtiene el total de gastos de un usuario.
     */
    @Query("SELECT SUM(monto) FROM gastos WHERE userId = :userId")
    suspend fun getTotalGastos(userId: String): Double?
    
    /**
     * Obtiene el total de gastos por categoría para un usuario.
     */
    @Query("SELECT SUM(monto) FROM gastos WHERE userId = :userId AND categoria = :categoria")
    suspend fun getTotalGastosByCategoria(userId: String, categoria: String): Double?
    
    /**
     * Obtiene el total de gastos en un rango de fechas.
     */
    @Query("SELECT SUM(monto) FROM gastos WHERE userId = :userId AND fecha BETWEEN :fechaInicio AND :fechaFin")
    suspend fun getTotalGastosByDateRange(userId: String, fechaInicio: Long, fechaFin: Long): Double?
    
    /**
     * Cuenta el número total de gastos de un usuario.
     */
    @Query("SELECT COUNT(*) FROM gastos WHERE userId = :userId")
    suspend fun getGastosCount(userId: String): Int
}
