package com.alexander.pasajes.ui.history;

public class CacheCleanupProcessor {

    public static final String STATUS_PURGE_OK = "DEPURACION_CACHE_CONFORME";
    public static final String STATUS_PROTECT_RECORDS = "BORRADO_OMITIDO_DATOS_PENDIENTES";
    public static final String STATUS_REDIRECT_ACTIVE = "REDIRECCION_PANTALLA_VENTA";

    /**
     * Evalúa analíticamente la seguridad de limpiar la base de datos local según el estado de sincronización.
     * @param boletosPendientes Cantidad de boletos en Room con flag 'sincronizado = false'.
     * @param turnoActivo Indica si la aplicación detecta una jornada que quedó abierta por un cierre inesperado.
     */
    public String evaluarLimpiezaCache(int boletosPendientes, boolean turnoActivo) {
        // [CP143]: Si la app se cerró por accidente pero hay un turno activo, se fuerza la redirección
        if (turnoActivo) {
            return STATUS_REDIRECT_ACTIVE;
        }

        // [CP143]: Si hay pasajes antiguos sin subir por falta de señal, se protege obligatoriamente el registro
        if (boletosPendientes > 0) {
            return STATUS_PROTECT_RECORDS;
        }

        // [CP142]: Cierre exitoso, copia en la nube conforme. Borra pasajes antiguos para mantener la app rápida
        return STATUS_PURGE_OK;
    }
}