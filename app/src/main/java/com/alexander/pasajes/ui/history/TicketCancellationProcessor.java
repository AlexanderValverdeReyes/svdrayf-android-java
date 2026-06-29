package com.alexander.pasajes.ui.history;

public class TicketCancellationProcessor {

    // Constantes de control y glosas exactas exigidas por la matriz de control de la docente
    public static final String STATUS_CANCEL_OK = "ANULACION_CONFORME";
    public static final String MSG_ERROR_ALREADY_CANCELLED = "Este pasaje ya fue anulado previamente.";
    public static final String MSG_ERROR_INSPECTOR = "Operación Ganada: El boleto seleccionado no puede ser anulado porque ya ha sido validado por el Inspector";

    /**
     * Evalúa la viabilidad contable y de auditoría para dar de baja un comprobante emitido.
     */
    public String evaluarAnulacionBoleto(boolean yaAnulado, boolean fueValidadoPorInspector) {
        // Control preventivo de redundancia
        if (yaAnulado) {
            return MSG_ERROR_ALREADY_CANCELLED;
        }

        // [CP90]: Candado de seguridad insalvable si el boleto ya fue escaneado por el fiscalizador en ruta
        if (fueValidadoPorInspector) {
            return MSG_ERROR_INSPECTOR;
        }

        // [CP89]: Flujo lícito para procesar la baja local del pasaje
        return STATUS_CANCEL_OK;
    }
}