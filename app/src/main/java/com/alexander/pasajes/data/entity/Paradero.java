package com.alexander.pasajes.data.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "paradero")
public class Paradero {
    @PrimaryKey
    @ColumnInfo(name = "id_paradero")
    public int id;

    @ColumnInfo(name = "nombre_paradero")
    public String nombre;

    public boolean estado; // no lo usaremos directamente, pero existe en la tabla
}