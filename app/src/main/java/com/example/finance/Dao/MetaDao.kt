package com.example.finance.Dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.finance.dataBase.entities.MetaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MetaDao {
    
    // INSERT
    
    /**
     * Inserta una nueva meta en la base de datos.
     * @return El ID de la meta insertada.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeta(meta: MetaEntity): Long
    
    /**
     * Inserta múltiples metas en la base de datos.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMetas(metas: List<MetaEntity>)
    
    // UPDATE
    
    /**
     * Actualiza una meta existente.
     */
    @Update
    suspend fun updateMeta(meta: MetaEntity)
    
    /**
     * Agrega dinero al monto ahorrado de una meta.
     */
    @Query("UPDATE metas SET ahorrado = ahorrado + :monto WHERE id = :metaId")
    suspend fun agregarDinero(metaId: Long, monto: Double)
    
    // DELETE
    
    /**
     * Elimina una meta específica.
     */
    @Delete
    suspend fun deleteMeta(meta: MetaEntity)
    
    /**
     * Elimina una meta por su ID.
     */
    @Query("DELETE FROM metas WHERE id = :metaId")
    suspend fun deleteMetaById(metaId: Long)
    
    /**
     * Elimina todas las metas de un usuario.
     */
    @Query("DELETE FROM metas WHERE userId = :userId")
    suspend fun deleteAllMetasByUser(userId: String)
    
    // SELECT
    
    /**
     * Obtiene todas las metas de un usuario como Flow (observable).
     */
    @Query("SELECT * FROM metas WHERE userId = :userId ORDER BY id DESC")
    fun getAllMetasByUser(userId: String): Flow<List<MetaEntity>>
    
    /**
     * Obtiene todas las metas de un usuario como lista.
     */
    @Query("SELECT * FROM metas WHERE userId = :userId ORDER BY id DESC")
    suspend fun getAllMetasByUserList(userId: String): List<MetaEntity>
    
    /**
     * Obtiene una meta por su ID.
     */
    @Query("SELECT * FROM metas WHERE id = :metaId")
    suspend fun getMetaById(metaId: Long): MetaEntity?
    
    /**
     * Obtiene metas completadas (ahorrado >= objetivo).
     */
    @Query("SELECT * FROM metas WHERE userId = :userId AND ahorrado >= objetivo")
    fun getMetasCompletadas(userId: String): Flow<List<MetaEntity>>
    
    /**
     * Obtiene metas pendientes (ahorrado < objetivo).
     */
    @Query("SELECT * FROM metas WHERE userId = :userId AND ahorrado < objetivo")
    fun getMetasPendientes(userId: String): Flow<List<MetaEntity>>
    
    /**
     * Cuenta el total de metas de un usuario.
     */
    @Query("SELECT COUNT(*) FROM metas WHERE userId = :userId")
    suspend fun getCountMetas(userId: String): Int
}
