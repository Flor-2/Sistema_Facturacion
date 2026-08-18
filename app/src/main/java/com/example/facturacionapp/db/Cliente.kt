package com.example.facturacionapp.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "Cliente_Entity")

data class Cliente(
    @PrimaryKey var rncCedula: Int,
    var nombre: String,
    var direccion: String
) : Serializable