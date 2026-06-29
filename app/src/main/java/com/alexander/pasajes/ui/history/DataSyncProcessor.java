package com.alexander.pasajes.ui.history;

public class DataSyncProcessor {

    public static final String STATUS_SYNC_OK = "Sincronización exitosa";
    public static final String MSG_ERROR_SIGNAL_DROP = "Conexión inestable. Intente de nuevo al recuperar señal";
    public static final String MSG_INFO_NO_PENDING = "Aviso: No existen registros pendientes de sincronización";

    /**
     * Evalúa analíticamente la viabilidad de la cola de subida masiva hacia Neon DB de forma incremental.
     * @param tieneInternet Indica si el canal de datos inalámbrico está activo al presionar el control.
     * @param boletosPendientes Cantidad de registros en Room cuyo flag 'sincronizado' es false.
     * @param corteSenalMedioProceso Flag de contingencia ante microcortes durante el volcado de buffers.
     */
    public String evaluarSincronizacion(boolean tieneInternet, int boletosPendientes, boolean corteSenalMedioProceso) {
        // [CP126]: Intento de sincronización redundante sin pasajes nuevos registrados en el terminal
        if (boletosPendientes == 0) {
            return MSG_INFO_NO_PENDING;
        }

        // [CP125]: Resguarda los pasajes intactos en Room local ante inestabilidad de la red celular
        if (!tieneInternet || corteSenalMedioProceso) {
            return MSG_ERROR_SIGNAL_DROP;
        }

        // [CP124]: Subida masiva de registros acumulados procesada correctamente por el backend
        return STATUS_SYNC_OK;
    }
}