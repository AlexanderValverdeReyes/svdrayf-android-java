package com.alexander.pasajes.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "tarifas")
public class Tarifa {
    @PrimaryKey
    public int id;
    public int rutaId;
    public int origenParaderoId;
    public int destinoParaderoId;
    public String tipoPasajero;
    public int precioCentavos;          // Precio normal en centavos
    public int precioDomFerCentavos;
}