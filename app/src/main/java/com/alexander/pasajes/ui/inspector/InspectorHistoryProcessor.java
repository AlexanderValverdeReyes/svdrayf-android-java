package com.alexander.pasajes.ui.inspector;
public class InspectorHistoryProcessor {

    // Glosas mandatorias e inmutables exigidas por los casos de prueba CP117 y CP118
    public static final String STATUS_READ_OK = "HISTORIAL_LISTO_PARA_DESPLIEGUE";
    public static final String MSG_EMPTY_HISTORY = "No se registran auditorías en la jornada actual";

    /**
     * Evalúa analíticamente el volumen de registros locales o remotos devueltos por el backend.
     */
    public String evaluarHistorialFiscalizaciones(int cantidadRegistros) {
        // [CP118]: Ausencia absoluta de revisiones de buses en la jornada actual
        if (cantidadRegistros == 0) {
            return MSG_EMPTY_HISTORY;
        }

        // [CP117]: Bitácoras recuperadas limpiamente desde Neon DB
        return STATUS_READ_OK;
    }
}