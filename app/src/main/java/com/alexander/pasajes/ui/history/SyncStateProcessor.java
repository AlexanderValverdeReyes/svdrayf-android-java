package com.alexander.pasajes.ui.history;

public class SyncStateProcessor {

    public static final String STATUS_PROGRESS_ACTIVE = "SYNC_TRANSMITIENDO_DATOS";
    public static final String STATUS_SYNC_FINISHED = "Sincronización exitosa y confirmada";
    public static final String MSG_ERROR_SYNC_PAUSED = "Sincronización pausada por falta de señal";

    /**
     * Determina el estado visual y operacional del canal de transmisión de datos hacia Neon DB.
     * @param estaTransmitiendo Indica si el socket o worker se encuentra enviando hilos de bytes.
     * @param tieneInternet Conectividad actual por antena celular o Wi-Fi.
     * @param cargaCompletada Confirmación de persistencia exitosa del servidor central.
     */
    public String evaluarEstadoSync(boolean estaTransmitiendo, boolean tieneInternet, boolean cargaCompletada) {
        // [CP128]: Desconexión abrupta de internet en plena transferencia (Congela barra y pausa el flujo)
        if (estaTransmitiendo && !tieneInternet) {
            return MSG_ERROR_SYNC_PAUSED;
        }

        // [CP127]: El proceso de envío de datos al servidor central está activo, transmitiendo e informando avance
        if (estaTransmitiendo && !cargaCompletada) {
            return STATUS_PROGRESS_ACTIVE;
        }

        // Cierre exitoso del ciclo de vida de la pasarela
        return STATUS_SYNC_FINISHED;
    }
}