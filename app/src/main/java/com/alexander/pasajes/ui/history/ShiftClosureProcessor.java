package com.alexander.pasajes.ui.history;

public class ShiftClosureProcessor {

    // Constantes de control analítico de hardware y conectividad cloud
    public static final String STATUS_SUCCESS_ONLINE = "CLOSURE_ONLINE_CONFORME";
    public static final String STATUS_OFFLINE_REDIRECT = "CLOSURE_OFFLINE_REDIRECCIONAR";
    public static final String STATUS_REPRINT_ALLOWED = "REIMPRESION_CIERRE_HABILITADA";
    public static final String STATUS_REPRINT_DENIED = "REIMPRESION_CIERRE_RECHAZADA";

    /**
     * Evalúa analíticamente la máquina de estados contable para la liquidación del viaje.
     */
    public String evaluarEstadoCierre(boolean tieneInternet, boolean errorMecanicoImpresora, boolean esIntentoReimpresion) {
        // [CP95]: Restricción arquitectónica de diseño: El cierre exige enlace cloud obligatorio
        if (!tieneInternet) {
            return STATUS_OFFLINE_REDIRECT;
        }

        if (esIntentoReimpresion) {
            // [CP96]: Permite la recuperación del reporte físico si la ticketera se quedó sin papel
            if (errorMecanicoImpresora) {
                return STATUS_REPRINT_ALLOWED;
            }
            return STATUS_REPRINT_DENIED;
        }

        // [CP94]: Cierre exitoso regular con bloqueo de emisión y subida de datos consolidados
        return STATUS_SUCCESS_ONLINE;
    }
}