package com.alexander.pasajes.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "motivos_anulacion")
public class MotivoAnulacion {
    @PrimaryKey
    public int id;
    public String descripcion;
}