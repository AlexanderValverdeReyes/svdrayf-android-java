package com.alexander.pasajes.network.model;

import com.google.gson.annotations.SerializedName;

public class ConfiguracionEmpresaDTO {
    @SerializedName("id_config")
    public int idConfig;
    @SerializedName("razon_social")
    public String razonSocial;
    public String ruc;
    @SerializedName("direccion_fiscal")
    public String direccionFiscal;
    @SerializedName("leyenda_pie")
    public String leyendaPie;
}