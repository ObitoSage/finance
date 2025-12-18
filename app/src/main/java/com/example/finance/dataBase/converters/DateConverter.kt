package com.example.finance.dataBase.converters

import androidx.room.TypeConverter
import java.util.Date

//Permite a Room almacenar tipos de datos que no soporta nativamente. 
//Room solo soporta tipos primitivos y String de forma nativa.
//Para Date u otros tipos complejos, necesitamos convertidores.

class DateConverter {
    
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }
    
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}
