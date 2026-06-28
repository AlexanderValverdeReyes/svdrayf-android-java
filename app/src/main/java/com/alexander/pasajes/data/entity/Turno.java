package com.alexander.pasajes.data.entity;

import androidx.room.Entity;
import androidx.room.ColumnInfo;
import androidx.room.PrimaryKey;

@Entity(tableName = "turnos")
public class Turno {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public int vendedorId;
    public int busId;
    public int rutaId;
    public long apertura;
    public long cierre;
    public boolean activo;
    @ColumnInfo(name = "sincronizado")
    public boolean sincronizado;

    // 🚀 ADICIÓN CRÍTICA: Guarda el id_turno real devuelto por PostgreSQL
    public int serverTurnoId;
}