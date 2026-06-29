package com.alexander.pasajes.network;

public class NetworkMonitoringProcessor {

    public static final String STATUS_AUTO_SYNC = "MONITOREO_SUBIDA_FONDO_INICIADA";
    public static final String STATUS_IDLE = "MONITOREO_REPOSO_CONFORME";

    public static final String MSG_INFO_CONNECTED = "Conexión estable detectada. Iniciando subida de fondo...";
    public static final String MSG_WARN_UNSTABLE = "Inestabilidad severa detectada. Pausando intentos automáticos por 10 segundos para cuidar batería.";

    /**
     * Monitorea el comportamiento del enlace celular y administra las ráfagas de subida de Room.
     * @param tieneInternet Estado actual del canal de datos (ConnectivityManager).
     * @param fluctuacionesSenoRecientes Cantidad de desconexiones consecutivas en un breve periodo.
     * @param boletosPendientes Cantidad de pasajes locales en Room con flag 'sincronizado = false'.
     */
    public String evaluarMonitoreoConexion(boolean tieneInternet, int fluctuacionesSenoRecientes, int boletosPendientes) {
        // [CP132]: Mitigación de inestabilidad severa de la señal celular (Evita drenaje de batería)
        if (fluctuacionesSenoRecientes >= 3) {
            return MSG_WARN_UNSTABLE;
        }

        // [CP131]: Detección automática de internet estable con registros pendientes de subida
        if (tieneInternet && boletosPendientes > 0) {
            return MSG_INFO_CONNECTED;
        }

        return STATUS_IDLE;
    }
}