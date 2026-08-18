package com.example.facturacionapp.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Factura::class, Producto::class, Cliente::class], version=1 )
abstract class FacturacionDataBase : RoomDatabase(){

    //abstract fun clienteDao(): ClienteDao
    //abstract fun productoDao(): ProductoDao
    abstract fun facturacionDao(): FacturacionDao
    companion object{
        @Volatile private var INSTANCIA: FacturacionDataBase?=null

         fun getDataBase(context: Context): FacturacionDataBase{
            return INSTANCIA?:synchronized(this){
                Room.databaseBuilder(
                    context.applicationContext,
                    FacturacionDataBase::class.java,
                    "facturacion_db"
                ).build().also{ INSTANCIA = it }
            }
        }
    }
}

