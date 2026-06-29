package com.alexander.pasajes.ui.sale;

public class VisualConfirmationProcessor {

    // Constantes de control analítico para la UI
    public static final String STATUS_CONFIRMED = "CONFIRMACION_VISUAL_VERDE";
    public static final String STATUS_FAILED = "CONFIRMACION_FALLIDA";

    /**
     * Valida analíticamente si corresponde desplegar el mensaje verde de confirmación en la pantalla.
     */
    public String evaluarConfirmacionVisual(boolean guardadoExitoso) {
        // [CP108]: El boleto se grabó correctamente en la base de datos local Room
        if (guardadoExitoso) {
            return STATUS_CONFIRMED;
        }

        return STATUS_FAILED;
    }
}