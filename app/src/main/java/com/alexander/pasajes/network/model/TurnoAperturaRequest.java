package com.alexander.pasajes.network.model;

public class TurnoAperturaRequest {
    private int id_bus;
    private int id_ruta_modalidad;

    public TurnoAperturaRequest(int id_bus, int id_ruta_modalidad) {
        this.id_bus = id_bus;
        this.id_ruta_modalidad = id_ruta_modalidad;
    }
}