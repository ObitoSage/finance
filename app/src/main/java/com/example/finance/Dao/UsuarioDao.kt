package com.example.finance.Dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.finance.dataBase.entities.UsuarioEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UsuarioDao {

    /**
     * Inserta un nuevo usuario o reemplaza si ya existe.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsuario(usuario: UsuarioEntity)

    /**
     * Actualiza un usuario existente.
     */
    @Update
    suspend fun updateUsuario(usuario: UsuarioEntity)

    /**
     * Obtiene un usuario por su ID.
     */
    @Query("SELECT * FROM usuarios WHERE userId = :userId")
    suspend fun getUsuarioById(userId: String): UsuarioEntity?

    /**
     * Obtiene un usuario por su ID como Flow para observar cambios.
     */
    @Query("SELECT * FROM usuarios WHERE userId = :userId")
    fun getUsuarioByIdFlow(userId: String): Flow<UsuarioEntity?>

    /**
     * Actualiza el presupuesto mensual de un usuario.
     */
    @Query("UPDATE usuarios SET presupuestoMensual = :presupuesto, fechaActualizacion = :fecha WHERE userId = :userId")
    suspend fun updatePresupuestoMensual(userId: String, presupuesto: Double, fecha: Long)

    /**
     * Obtiene el presupuesto mensual de un usuario.
     */
    @Query("SELECT presupuestoMensual FROM usuarios WHERE userId = :userId")
    suspend fun getPresupuestoMensual(userId: String): Double?

    /**
     * Verifica si un usuario existe.
     */
    @Query("SELECT COUNT(*) FROM usuarios WHERE userId = :userId")
    suspend fun existsUsuario(userId: String): Int

    /**
     * Elimina un usuario por su ID.
     */
    @Query("DELETE FROM usuarios WHERE userId = :userId")
    suspend fun deleteUsuarioById(userId: String)
}

