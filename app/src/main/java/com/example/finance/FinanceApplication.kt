package com.example.finance

import android.app.Application
import com.example.finance.dataBase.AppDatabase
import com.example.finance.dataBase.repository.FinanceRepository

/**
 * Clase Application personalizada para inicializar componentes globales.
 * Proporciona acceso singleton a la base de datos y al repository.
 */
 
class FinanceApplication : Application() {
    
    //Se crea solo cuando se accede por primera vez.
    val database: AppDatabase by lazy { 
        AppDatabase.getDatabase(this) 
    }

    val repository: FinanceRepository by lazy { 
        FinanceRepository(
            gastoDao = database.gastoDao(),
            ingresoDao = database.ingresoDao(),
            usuarioDao = database.usuarioDao()
        )
    }
}
