package com.alexander.pasajes.network.model;

import com.google.gson.annotations.SerializedName;

public class LoginResponse {
    @SerializedName("status")
    private String status;
    @SerializedName("token")
    private String token;
    @SerializedName("usuario")
    private UsuarioDTO usuario;

    public String getStatus() { return status; }
    public String getToken() { return token; }
    public UsuarioDTO getUsuario() { return usuario; }
}