package com.alexander.pasajes.network;

public class NetworkSemaphoreProcessor {

    public static final String COLOR_VERDE = "VERDE";     // Con internet y todo sincronizado
    public static final String COLOR_NARANJA = "NARANJA"; // Con internet pero con datos guardados sin subir
    public static final String COLOR_ROJO = "ROJO";       // Sin internet (Estado Offline)

    /**
     * Evalúa automáticamente las variables de infraestructura y almacenamiento local
     * para determinar el color exacto del indicador de la cabecera.
     * * @param tieneInternet Conectividad actual devuelta por el ConnectivityManager.
     * @param boletosPendientes Cantidad de boletos en Room con flag 'sincronizado = false'.
     */
    public String evaluarEstadoRed(boolean tieneInternet, int boletosPendientes) {
        // [CP130]: Desconexión total de datos móviles (Cambio inmediato a ROJO)
        if (!tieneInternet) {
            return COLOR_ROJO;
        }

        // [CP129]: Cuenta con internet pero detecta pasajes acumulados pendientes de subida
        if (boletosPendientes > 0) {
            return COLOR_NARANJA;
        }

        // [CP129]: Escenario Exitoso — Progreso conforme, online y base de datos limpia
        return COLOR_VERDE;
    }
}