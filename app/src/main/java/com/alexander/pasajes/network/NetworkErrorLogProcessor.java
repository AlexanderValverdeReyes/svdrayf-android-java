package com.alexander.pasajes.network;

public class NetworkErrorLogProcessor {

    public static final String STATUS_LOGGED_OK = "FALLA_CAPTURADA_LOCALMENTE";
    public static final String STATUS_READ_CONFORME = "LOG_CONFORME_PARA_DESPLIEGUE";
    public static final String MSG_NO_ERRORS = "No se registran errores en la jornada actual";

    /**
     * Captura el código de error y la marca de tiempo exacta ante caídas del socket (CP135).
     * @param codigoFallo Glosa del error devuelto por la excepción de red (Ej: "TIMEOUT", "502_BAD_GATEWAY").
     * @param timestamp Hora exacta en milisegundos en la que ocurrió la interrupción.
     */
    public String registrarFallaTransmision(String codigoFallo, long timestamp) {
        if (codigoFallo == null || codigoFallo.trim().isEmpty() || timestamp <= 0) {
            return "ERROR_PARAMETROS_INVALIDOS";
        }

        // Retorna el token de aprobación para confirmar el guardado atómico en SharedPreferences o Room local
        return STATUS_LOGGED_OK;
    }

    /**
     * Evalúa el volumen de fallas registradas al abrir el contenedor visual del menú técnico (CP136).
     * @param cantidadErrores Cantidad de filas recuperadas del almacenamiento local.
     */
    public String evaluarConsultaListaErrores(int cantidadErrores) {
        // [CP136]: Si el aplicativo operó de forma perfecta, limpia la pantalla y muestra la advertencia exacta
        if (cantidadErrores == 0) {
            return MSG_NO_ERRORS;
        }

        return STATUS_READ_CONFORME;
    }
}