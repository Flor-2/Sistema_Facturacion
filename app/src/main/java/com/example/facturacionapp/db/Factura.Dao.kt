package com.example.facturacionapp.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow


@Dao
interface FacturacionDao {
    @Query("SELECT * FROM producto_entity WHERE idProducto = :id")
    fun getProductoById(id: Int): Producto?

    @Insert
    fun insertProducto(producto: Producto): Long

    // Consultas de Clientes
    @Query("SELECT * FROM cliente_entity WHERE rncCedula = :rnc")
    fun getClienteByRnc(rnc: Int): Cliente?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertCliente(cliente: Cliente)

    // Registrar Factura
    @Insert
    fun insertFactura(factura: Factura): Long

    @Update
    fun updateProducto(producto: Producto)
}