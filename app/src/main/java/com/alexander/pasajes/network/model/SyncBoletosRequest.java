package com.alexander.pasajes.network.model;

import java.util.List;

public class SyncBoletosRequest {
    private List<BoletoSync> boletos;
    public SyncBoletosRequest(List<BoletoSync> boletos) { this.boletos = boletos; }
}