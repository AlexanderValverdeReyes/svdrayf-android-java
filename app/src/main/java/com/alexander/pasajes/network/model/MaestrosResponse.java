package com.alexander.pasajes.network.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class MaestrosResponse {
    @SerializedName("status")
    private String status;

    @SerializedName("data")
    private Data data;

    public List<BusDTO> getBuses() {
        return data.buses;
    }

    public List<RutaDTO> getRutas() {
        return data.rutas;
    }

    public List<ParaderoDTO> getParaderos() {
        return data.paraderos;
    }

    public List<TarifaDTO> getTarifas() {
        return data.tarifario;
    }

    public List<MotivoAnulacionDTO> getMotivosAnulacion() {
        return data.motivos_anulacion;
    }

    public ConfiguracionEmpresaDTO getConfiguracion() {
        return data.configuracion;
    }

    public List<TipoPasajeroDTO> getTiposPasajero() {
        return data.tiposPasajero;
    }

    static class Data {
        List<BusDTO> buses;
        List<RutaDTO> rutas;
        List<ParaderoDTO> paraderos;
        @SerializedName("tarifario")
        List<TarifaDTO> tarifario;
        @SerializedName("tipos_pasajero")
        List<TipoPasajeroDTO> tiposPasajero;
        @SerializedName("motivos_anulacion")
        List<MotivoAnulacionDTO> motivos_anulacion;
        ConfiguracionEmpresaDTO configuracion;
    }
}