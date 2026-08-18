package com.example.facturacionapp.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "Producto_Entity")
data class Producto(
    @PrimaryKey(autoGenerate = true)
    var idProducto: Int = 0,
    var nombre: String,
    var precio: Double,
    var stock: Int
) : Serializable