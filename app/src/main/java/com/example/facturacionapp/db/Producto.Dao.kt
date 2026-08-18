package com.example.facturacionapp.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductoDao{

    @Query("SELECT * FROM Producto_Entity")
    fun getAll(): List<Producto>

    @Query("SELECT * FROM Producto_Entity WHERE idProducto = :id")
    fun getById(id: Int) : Producto?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(producto: Producto)

    @Update
    fun update(producto: Producto)

    @Delete
    fun delete(producto: Producto)

}