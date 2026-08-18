package com.example.facturacionapp.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ClienteDao{

    @Query("SELECT * FROM Cliente_Entity")
    fun getAll(): Flow<List<Cliente>>

    @Query("SELECT * FROM Cliente_Entity WHERE rncCedula=:id")
    fun getById(id: Int) : Flow<Cliente>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(producto: Cliente)

    @Update
    fun update(producto: Cliente)

    @Delete
    fun delete(producto: Cliente)

}