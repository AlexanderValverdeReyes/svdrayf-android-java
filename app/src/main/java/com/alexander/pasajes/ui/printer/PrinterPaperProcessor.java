package com.alexander.pasajes.ui.printer;

public class PrinterPaperProcessor {

    // Constantes de control y glosas exactas exigidas por tu matriz Excel
    public static final String STATUS_PAPER_OK = "PAPER_VALIDADO_CONFORME";
    public static final String MSG_WARN_SENSOR_INCOMPATIBLE = "Nota: No se pudo verificar el nivel de papel";
    public static final String MSG_ERROR_NO_PAPER = "Alerta: La ticketera no detecta rollo térmico. Inserte papel para continuar.";

    /**
     * Evalúa analíticamente el estado del sensor de papel de la mini-ticketera de 58mm.
     * @param sensorCompatible Indica si el firmware de la impresora responde los comandos de estado de papel.
     * @param tienePapelSuficiente Telemetría física del estado del rodillo térmico.
     */
    public String evaluarEstadoPapel(boolean sensorCompatible, boolean tienePapelSuficiente) {
        // [CP107]: Si el sensor es inoperativo o incompatible, se ignora la lectura y se permite imprimir con aviso
        if (!sensorCompatible) {
            return MSG_WARN_SENSOR_INCOMPATIBLE;
        }

        // Validación de seguridad si el sensor es sano pero falta insumo físico
        if (!tienePapelSuficiente) {
            return MSG_ERROR_NO_PAPER;
        }

        // [CP106]: Verificación correcta de papel, rollo compatible listo para imprimir sin alertas
        return STATUS_PAPER_OK;
    }
}