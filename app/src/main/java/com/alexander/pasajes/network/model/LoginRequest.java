package com.alexander.pasajes.network.model;

public class LoginRequest {
    private String identificador;
    private String password;

    public LoginRequest(String identificador, String password) {
        this.identificador = identificador;
        this.password = password;
    }
}