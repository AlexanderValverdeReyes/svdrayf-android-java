package com.alexander.pasajes.ui.sale;

public class AuditAlertProcessor {

    // Constantes de control interno para el motor analítico de auditoría
    public static final String STATUS_CONFORME = "AUDIT_CONFORME";
    public static final String STATUS_ALERTA_FRAUDE = "AUDIT_ALERTA_DISPARADA";

    /**
     * Evalúa el comportamiento transaccional del cobrador para identificar anomalías de pasarela.
     * * @param abrioQrPreviamente Indica si la interfaz llegó a proyectar el QR en el pasillo.
     * @param metodoPagoFinal El método de pago con el que se confirma físicamente la venta.
     * @param cantidadCambios El número de conmutaciones registradas en el RadioGroup.
     */
    public String evaluarAlertaAuditoria(boolean abrioQrPreviamente, String metodoPagoFinal, int cantidadCambios) {
        // [CP85]: Cambio múltiple o intento de QR revertido a efectivo a último momento (Indicios de desvío de caja)
        if (cantidadCambios > 1 || (abrioQrPreviamente && "EFECTIVO".equals(metodoPagoFinal))) {
            return STATUS_ALERTA_FRAUDE;
        }

        // [CP84]: Selección directa y limpia del método solicitado por el pasajero
        return STATUS_CONFORME;
    }
}