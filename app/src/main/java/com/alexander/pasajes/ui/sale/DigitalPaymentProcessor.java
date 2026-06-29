package com.alexander.pasajes.ui.sale;

public class DigitalPaymentProcessor {

    // Glosas explícitas e inmutables exigidas por tu ficha de control
    public static final String STATUS_QR_DISPLAYED = "QR_DESPLEGADO_EXITOSO";
    public static final String STATUS_CASH_CONMUTED = "CAMBIO_A_EFECTIVO_REGISTRADO";
    public static final String MSG_ERROR_QR_MISSING = "Error de Hardware: Código QR estático de la unidad vehicular no disponible localmente. Módulo digital suspendido temporalmente. Por favor, proceda a realizar el cobro del pasaje en modal Efectivo";

    /**
     * Evalúa la integridad y consistencia del canal de preventa digital y efectivo.
     */
    public String evaluarModalidadPago(String metodoSeleccionado, boolean qrDisponibleEnMemoria, boolean cambioUltimoMomento) {
        // [CP83]: Escudo preventivo si el archivo físico del QR estático se borró o dañó en el celular
        if ("QR".equals(metodoSeleccionado)) {
            if (!qrDisponibleEnMemoria) {
                return MSG_ERROR_QR_MISSING;
            }
            return STATUS_QR_DISPLAYED; // [CP81]
        }

        // [CP82]: Registro de control contable para alternancias por cambio de parecer del usuario
        if ("EFECTIVO".equals(metodoSeleccionado) && cambioUltimoMomento) {
            return STATUS_CASH_CONMUTED;
        }

        return "METODO_CONFORME";
    }
}