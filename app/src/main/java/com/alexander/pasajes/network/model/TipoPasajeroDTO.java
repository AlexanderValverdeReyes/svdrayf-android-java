package com.alexander.pasajes.network.model;

import com.google.gson.annotations.SerializedName;

public class TipoPasajeroDTO {
    @SerializedName("id_tipos_pasajero")
    public int idTipoPasajero;

    @SerializedName("nombre_tipo")
    public String nombreTipo;
}