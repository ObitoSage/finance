package com.example.finance.dataBase.repository

import com.example.finance.Dao.GastoDao
import com.example.finance.Dao.IngresoDao
import com.example.finance.dataBase.entities.GastoEntity
import com.example.finance.dataBase.entities.IngresoEntity
import kotlinx.coroutines.flow.Flow

//Repository que actúa como una capa de abstracción entre los DAOs y el resto de la app.

class FinanceRepository(
    private val gastoDao: GastoDao,
    private val ingresoDao: IngresoDao) {
    
    //GASTOS
    fun getAllGastos(userId: String): Flow<List<GastoEntity>> = 
        gastoDao.getAllGastosByUser(userId)

    suspend fun getAllGastosList(userId: String): List<GastoEntity> = 
        gastoDao.getAllGastosByUserList(userId)

    suspend fun insertGasto(gasto: GastoEntity): Long = 
        gastoDao.insertGasto(gasto)
    
    suspend fun updateGasto(gasto: GastoEntity) = 
        gastoDao.updateGasto(gasto)
    
    suspend fun deleteGasto(gasto: GastoEntity) = 
        gastoDao.deleteGasto(gasto)

    suspend fun deleteGastoById(id: Long) = 
        gastoDao.deleteGastoById(id)

    suspend fun getGastoById(id: Long): GastoEntity? = 
        gastoDao.getGastoById(id)
    
    fun getGastosByCategoria(userId: String, categoria: String): Flow<List<GastoEntity>> = 
        gastoDao.getGastosByCategoria(userId, categoria)
    
    fun getGastosByDateRange(userId: String, fechaInicio: Long, fechaFin: Long): Flow<List<GastoEntity>> = 
        gastoDao.getGastosByDateRange(userId, fechaInicio, fechaFin)
    
    suspend fun getTotalGastos(userId: String): Double = 
        gastoDao.getTotalGastos(userId) ?: 0.0

    suspend fun getTotalGastosByCategoria(userId: String, categoria: String): Double = 
        gastoDao.getTotalGastosByCategoria(userId, categoria) ?: 0.0
    
    //INGRESOS

    fun getAllIngresos(userId: String): Flow<List<IngresoEntity>> = 
        ingresoDao.getAllIngresosByUser(userId)

    suspend fun getAllIngresosList(userId: String): List<IngresoEntity> = 
        ingresoDao.getAllIngresosByUserList(userId)

    suspend fun insertIngreso(ingreso: IngresoEntity): Long = 
        ingresoDao.insertIngreso(ingreso)
    
    suspend fun updateIngreso(ingreso: IngresoEntity) = 
        ingresoDao.updateIngreso(ingreso)
    
    suspend fun deleteIngreso(ingreso: IngresoEntity) = 
        ingresoDao.deleteIngreso(ingreso)
    
    suspend fun deleteIngresoById(id: Long) = 
        ingresoDao.deleteIngresoById(id)

    suspend fun getIngresoById(id: Long): IngresoEntity? = 
        ingresoDao.getIngresoById(id)
    
    fun getIngresosByCategoria(userId: String, categoria: String): Flow<List<IngresoEntity>> = 
        ingresoDao.getIngresosByCategoria(userId, categoria)

    fun getIngresosByDateRange(userId: String, fechaInicio: Long, fechaFin: Long): Flow<List<IngresoEntity>> = 
        ingresoDao.getIngresosByDateRange(userId, fechaInicio, fechaFin)

    suspend fun getTotalIngresos(userId: String): Double = 
        ingresoDao.getTotalIngresos(userId) ?: 0.0
 
    suspend fun getTotalIngresosByCategoria(userId: String, categoria: String): Double = 
        ingresoDao.getTotalIngresosByCategoria(userId, categoria) ?: 0.0
    
    //BALANCE

    suspend fun getBalance(userId: String): Double {
        val totalIngresos = getTotalIngresos(userId)
        val totalGastos = getTotalGastos(userId)
        return totalIngresos - totalGastos
    }
}
