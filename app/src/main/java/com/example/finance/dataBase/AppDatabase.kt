package com.example.finance.dataBase

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.finance.Dao.GastoDao
import com.example.finance.Dao.IngresoDao
import com.example.finance.Dao.UsuarioDao
import com.example.finance.dataBase.entities.GastoEntity
import com.example.finance.dataBase.entities.IngresoEntity
import com.example.finance.dataBase.entities.UsuarioEntity

//Usa el patrón Singleton para garantizar una única instancia de la base de datos.

@Database(
    entities = [
        GastoEntity::class,
        IngresoEntity::class,
        UsuarioEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun gastoDao(): GastoDao
    abstract fun ingresoDao(): IngresoDao
    abstract fun usuarioDao(): UsuarioDao

    companion object {
        private const val DATABASE_NAME = "finance_database"
        
        // Volatile garantiza que la instancia sea visible para todos los threads
        @Volatile
        private var INSTANCE: AppDatabase? = null
        fun getDatabase(context: Context): AppDatabase {
            // Si la instancia ya existe, la retorna
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    // Estrategia de migración destructiva (borra datos si cambia el schema)
                    .fallbackToDestructiveMigration()
                    .build()
                
                INSTANCE = instance
                instance
            }
        }
    }
}
