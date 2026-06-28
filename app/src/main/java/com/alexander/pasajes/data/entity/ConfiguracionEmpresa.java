package com.alexander.pasajes.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "configuracion_empresa")
public class ConfiguracionEmpresa {
    @PrimaryKey
    public int id;
    public String razonSocial;
    public String ruc;
    public String direccionFiscal;
    public String leyendaPie;
}