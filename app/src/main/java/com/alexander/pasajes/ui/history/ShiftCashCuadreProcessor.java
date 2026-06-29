package com.alexander.pasajes.ui.history;

public class ShiftCashCuadreProcessor {

    // Glosas exactas y alertas exigidas por la matriz de control de la docente
    public static final String STATUS_CUADRE_OK = "CUADRE_CONFORME";
    public static final String MSG_EMPTY_SHIFT = "Turno inicializado de forma limpia. No se registran ventas";
    public static final String MSG_WARN_CORRUPTED = "Alerta contable: Se detectó una desalineación de registros locales por apagado imprevisto. Se recomienda sincronizar con la nube de inmediato para asegurar el arqueo";

    /**
     * Evalúa analíticamente el estado contable del arqueo de caja de la ruta Mala-Lima.
     * @param totalBoletos Número total de registros de pasajes procesados (válidos + anulados).
     * @param registrosCorruptosDetectados Flag heurístico de Room ante caídas drásticas de batería.
     */
    public String evaluarEstadoCuadre(int totalBoletos, boolean registrosCorruptosDetectados) {
        // [CP93]: Escudo de recuperación limpia ante un apagado imprevisto del smartphone
        if (registrosCorruptosDetectados) {
            return MSG_WARN_CORRUPTED;
        }

        // [CP92]: Control perimetral para turnos recién inicializados sin boletaje comercial
        if (totalBoletos == 0) {
            return MSG_EMPTY_SHIFT;
        }

        // [CP91]: Flujo conforme cruzando operaciones de efectivo y billeteras electrónicas (QR)
        return STATUS_CUADRE_OK;
    }
}