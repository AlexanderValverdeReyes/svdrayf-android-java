package com.alexander.pasajes.network.model;

import com.google.gson.annotations.SerializedName;

public class BusDTO {
    @SerializedName("id_bus")
    public int idBus;
    public String placa;
    @SerializedName("numero_padron")
    public String numeroPadron;
    @SerializedName("capacidad_pasajeros")
    public int capacidadPasajeros;
    // Añade más campos si los necesitas
}