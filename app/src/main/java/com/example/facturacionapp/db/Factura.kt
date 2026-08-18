package com.example.facturacionapp.db
import android.R
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "Factura_Entity")
data class Factura(
    @PrimaryKey (autoGenerate = true)
    var idFactura: Int = 0,
    var rncCedula: Int,
    var idProducto: Int,
    var cantidad: Int,
    var Total: Double,
    var fecha: String
) : Serializable






