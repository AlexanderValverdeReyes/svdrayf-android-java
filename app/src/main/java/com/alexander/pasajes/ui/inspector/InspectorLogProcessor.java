package com.alexander.pasajes.ui.inspector;
public class InspectorLogProcessor {

    public static final String STATUS_LOG_CONFORME = "LOG_CONFORME";
    public static final String MSG_ERROR_ZERO_OR_EMPTY = "Error: Ingrese una cantidad mayor a cero";
    public static final String MSG_ERROR_EMPTY_OBSERVATION = "Debe ingresar una observación manual detallando la irregularidad encontrada.";

    /**
     * Evalúa la integridad de los datos ingresados por el fiscalizador antes de enviarlos a la central web.
     */
    public String evaluarBitacoraControl(int pasajerosSilicosis, String observaciones, boolean isBoletoValidoYActivo) {
        // [CP116]: Validación mandatoria si el campo de pasajeros se deja vacío, en cero o negativo
        if (pasajerosSilicosis <= 0) {
            return MSG_ERROR_ZERO_OR_EMPTY;
        }

        // [CP116]: Validación de contingencia si existen irregularidades pero no se detalla la observación
        if (!isBoletoValidoYActivo && (observaciones == null || observaciones.trim().isEmpty())) {
            return MSG_ERROR_EMPTY_OBSERVATION;
        }

        // [CP115]: Registro correcto de bitácora listo para guardarse en Room local y cloud
        return STATUS_LOG_CONFORME;
    }
}