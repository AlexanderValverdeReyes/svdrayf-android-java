package com.alexander.pasajes.network.model;

import com.google.gson.annotations.SerializedName;

public class UsuarioDTO {
    @SerializedName("id_usuario")
    private int idUsuario;
    @SerializedName("nombres")
    private String nombres;
    @SerializedName("id_rol")
    private int idRol;
    @SerializedName("requiere_cambio")
    private boolean requiereCambio;

    public int getIdUsuario() { return idUsuario; }
    public String getNombres() { return nombres; }
    public int getIdRol() { return idRol; }
    public boolean isRequiereCambio() { return requiereCambio; }
}