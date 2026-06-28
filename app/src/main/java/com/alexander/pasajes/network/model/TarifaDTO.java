package com.alexander.pasajes.network.model;

import com.google.gson.annotations.SerializedName;

public class TarifaDTO {
    @SerializedName("id_tarifario")
    public int idTarifario;
    @SerializedName("id_ruta_modalidad")
    public int idRutaModalidad;
    @SerializedName("id_paradero_origen")
    public int idParaderoOrigen;
    @SerializedName("id_paradero_destino")
    public int idParaderoDestino;
    @SerializedName("id_tipo_pasajero")
    public int idTipoPasajero;
    @SerializedName("precio_normal_centavos")
    public int precioNormalCentavos;
    @SerializedName("precio_dom_fer_centavos")
    public int precioDomFerCentavos;
}