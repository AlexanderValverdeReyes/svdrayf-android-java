package com.alexander.pasajes.network.model;

import com.google.gson.annotations.SerializedName;

public class MotivoAnulacionDTO {
    @SerializedName("id_motivo")
    public int idMotivo;
    @SerializedName("descripcion_motivo")
    public String descripcionMotivo;
}