package com.alexander.pasajes.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "rutas")
public class Ruta {
    @PrimaryKey
    public int id;
    public String nombre;
    public String tipo; // "DIRECTO" o "PARADEROS"

    @Override
    public String toString() {
        return this.nombre; // ✅ Esto hará que el Spinner muestre el nombre de la Ruta (ej: Mala - Lima)
    }
}