package com.alexander.pasajes.network.model;

import com.google.gson.annotations.SerializedName;

public class BuscarBoletoResponse {
    @SerializedName("status")
    public String status;

    // Ahora es un objeto único, no una lista
    @SerializedName("data")
    public BoletoEncontrado data;

    public static class BoletoEncontrado {
        @SerializedName("id_boleto")
        public String idBoleto;
        @SerializedName("id_turno")
        public int idTurno;

        //  ADICIÓN: Captura si la jornada o el turno del viaje sigue ABIERTO o CERRADO
        @SerializedName("estado_turno")
        public String estadoTurno;

        @SerializedName("hash_qr")
        public String hashQr;
        @SerializedName("estado_boleto")
        public String estadoBoleto;
        @SerializedName("monto_pagado_centavos")
        public int montoPagadoCentavos;
        @SerializedName("fecha_emision")
        public String fechaEmision;
        @SerializedName("modalidad_pago")
        public String modalidadPago;
        @SerializedName("cobrador")
        public String cobrador;
        @SerializedName("placa")
        public String placa;
        @SerializedName("numero_padron")
        public String numeroPadron;
        @SerializedName("ruta")
        public String ruta;
        @SerializedName("origen")
        public String origen;
        @SerializedName("destino")
        public String destino;
    }
}