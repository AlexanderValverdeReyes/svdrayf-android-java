package com.alexander.pasajes.network.model;

import com.google.gson.annotations.SerializedName;

public class ParaderoDTO {
    @SerializedName("id_paradero")
    public int idParadero;
    @SerializedName("nombre_paradero")
    public String nombreParadero;
}