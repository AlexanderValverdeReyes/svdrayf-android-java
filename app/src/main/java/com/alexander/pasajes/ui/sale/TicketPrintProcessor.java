package com.alexander.pasajes.ui.sale;

public class TicketPrintProcessor {

    // Estados de control del hardware de impresión
    public static final String STATUS_PRINT_OK = "IMPRESION_EXITOSA";
    public static final String STATUS_CAN_REPRINT_JAM = "REIMPRESION_HABILITADA_ATASCO";
    public static final String STATUS_CAN_REPRINT_DISCONNECT = "REIMPRESION_HABILITADA_DESCONEXION";
    public static final String STATUS_DENIED = "REIMPRESION_DENEGADA_SEGURIDAD";

    // Glosa obligatoria exigida por auditoría interna
    public static final String GLOSA_ATASCO = "\n--- EMISIÓN POR RECUPERACIÓN DE ATASCO ---\n";

    /**
     * Evalúa de forma estricta si el periférico califica para un proceso de reimpresión forzada.
     * Evita la clonación deliberada de comprobantes en el pasillo del bus.
     */
    public String evaluarSolicitudImpresion(boolean boletoCobrado, boolean errorMecanicoPapel, boolean errorConexionBluetooth, boolean intentoReimpresion) {
        if (!boletoCobrado) {
            return STATUS_DENIED;
        }

        if (intentoReimpresion) {
            if (errorMecanicoPapel) {
                return STATUS_CAN_REPRINT_JAM; // [CP87]
            }
            if (errorConexionBluetooth) {
                return STATUS_CAN_REPRINT_DISCONNECT; // [CP88]
            }
            return STATUS_DENIED; // Bloqueo preventivo si el hardware está totalmente sano
        }

        return STATUS_PRINT_OK; // [CP86] Impresión regular inicial
    }

    /**
     * Inyecta la marca de agua de auditoría al payload de impresión si corresponde a una recuperación física.
     */
    public String formatearPayloadSeguro(String contenidoOriginal, boolean esRecuperacionAtasco) {
        if (esRecuperacionAtasco) {
            return contenidoOriginal + GLOSA_ATASCO;
        }
        return contenidoOriginal;
    }
}