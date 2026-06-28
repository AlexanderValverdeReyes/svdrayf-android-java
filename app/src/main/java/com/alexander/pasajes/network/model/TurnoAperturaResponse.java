package com.alexander.pasajes.network.model;
import com.google.gson.annotations.SerializedName;

public class TurnoAperturaResponse {
    @SerializedName("status")
    public String status;
    @SerializedName("message")
    public String message;
    @SerializedName("turno")
    public TurnoDTO turno;

    public static class TurnoDTO {
        @SerializedName("id_turno")
        public int idTurno;
    }
}