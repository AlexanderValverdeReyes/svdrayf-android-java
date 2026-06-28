package com.alexander.pasajes.network.model;

public class BoletoSync {
    public String id_boleto; // UUID
    public int id_turno;
    public int id_tarifario; // Puede ser 0 si no se usa
    public int monto_pagado_centavos;
    public String modalidad_pago;
    public String estado_boleto; // "VALIDO" o "ANULADO"
    public String hash_qr;
    public boolean alerta_auditoria_qr;
    public String fecha_emision; // ISO 8601
    public boolean es_reimpresion;
    public String id_boleto_original;
    public Integer id_motivo_anulacion;
    public String fecha_anulacion;


}