package com.alexander.pasajes.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;

@Entity(tableName = "buses")
public class Bus {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String placa;
    public String descripcion;

    @ColumnInfo(name = "numero_padron")
    public String numero_padron;

    @ColumnInfo(name = "estado")
    public boolean estado;

    @Override
    public String toString() {
        return this.placa;
    }
}