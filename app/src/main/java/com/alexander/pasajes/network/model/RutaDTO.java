package com.alexander.pasajes.network.model;

import com.google.gson.annotations.SerializedName;

public class RutaDTO {
    @SerializedName("id_ruta_modalidad")
    public int idRutaModalidad;
    @SerializedName("nombre_modalidad")
    public String nombreModalidad;
}