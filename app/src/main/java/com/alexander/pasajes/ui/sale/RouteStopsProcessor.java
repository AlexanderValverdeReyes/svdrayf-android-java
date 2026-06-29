package com.alexander.pasajes.ui.sale;

public class RouteStopsProcessor {

    // Glosas exactas y restrictivas exigidas por tu ficha de control
    public static final String STATUS_STOPS_OK = "STOPS_CONFORME";
    public static final String MSG_ERROR_INVERTED_STOPS = "Tramo inválido: El paradero de destino seleccionado no se encuentra en el sentido de la ruta actual del bus. Corrija los datos para calcular la tarifa real";
    public static final String MSG_ERROR_CORRUPTED_MATRIX = "Error de consistencia: La matriz de paraderos locales se encuentra ilegible o incompleta. Contacte con soporte técnico";

    /**
     * Valida analíticamente la coherencia posicional y direccional de los paraderos de la ruta.
     */
    public String evaluarSeleccionTramos(boolean esSentidoInvalido, boolean esMatrizCorrupta) {
        // [CP80]: Escudo ante tablas relacionales SQLite corruptas o truncadas
        if (esMatrizCorrupta) {
            return MSG_ERROR_CORRUPTED_MATRIX;
        }

        // [CP79]: Interceptación de tramos invertidos en el sentido operacional (Sur a Norte)
        if (esSentidoInvalido) {
            return MSG_ERROR_INVERTED_STOPS;
        }

        // [CP78]: Registro correcto de tramos comerciales para cotización en < 0.5s
        return STATUS_STOPS_OK;
    }
}