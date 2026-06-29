package com.alexander.pasajes.ui.history;

public class DataConflictProcessor {

    // Constantes inmutables de control para la máquina de sincronización
    public static final String STATUS_CONSERVAR_RECIENTE = "CONSERVAR_REGISTRO_RECIENTE";
    public static final String STATUS_IGNORAR_DUPLICADO = "IGNORAR_REGISTRO_REPETIDO";
    public static final String STATUS_PROCESAMIENTO_CONFORME = "PROCESAMIENTO_CONFORME";

    /**
     * Evalúa analíticamente las colisiones de llaves primarias o hash durante el volcado al servidor central.
     * @param mismoCodigo Indica si el ID de boleto o UUID ya existe en las tablas del servidor.
     * @param datosDiferentes Flag de integridad que denota diferencias estructurales en el payload.
     * @param fechaLocal Marca de tiempo Unix del registro almacenado en el Room del smartphone.
     * @param fechaServidor Marca de tiempo Unix del registro persistido en Neon DB.
     * @param esIdentico Indica si la fila entrante posee la misma firma exacta que el registro cloud.
     */
    public String evaluarResolucionConflicto(boolean mismoCodigo, boolean datosDiferentes, long fechaLocal, long fechaServidor, boolean esIdentico) {
        // [CP133]: Conciliación de registros por fecha (Conserva el pasaje con la hora más reciente)
        if (mismoCodigo && datosDiferentes) {
            if (fechaLocal >= fechaServidor) {
                return STATUS_CONSERVAR_RECIENTE;
            }
        }

        // [CP134]: Intento de subida con datos exactamente idénticos (Omitir para evitar duplicar ingresos)
        if (esIdentico) {
            return STATUS_IGNORAR_DUPLICADO;
        }

        return STATUS_PROCESAMIENTO_CONFORME;
    }
}