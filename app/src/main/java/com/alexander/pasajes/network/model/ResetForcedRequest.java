package com.alexander.pasajes.network.model;

import com.google.gson.annotations.SerializedName;

public class ResetForcedRequest {

    @SerializedName("identificador")
    private String identificador;

    @SerializedName("token")
    private String token;

    @SerializedName("newPassword")
    private String newPassword;

    public ResetForcedRequest(String identificador, String token, String newPassword) {
        this.identificador = identificador;
        this.token = token;
        this.newPassword = newPassword;
    }

    public String getIdentificador() {
        return identificador;
    }

    public void setIdentificador(String identificador) {
        this.identificador = identificador;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}