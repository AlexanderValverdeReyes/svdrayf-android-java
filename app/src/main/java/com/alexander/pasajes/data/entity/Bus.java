package com.alexander.pasajes.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "buses")
public class Bus {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String placa;
    public String descripcion;

    @Override
    public String toString() {
        return this.placa; // ✅ Esto hará que el Spinner muestre la Placa limpia
    }
}
