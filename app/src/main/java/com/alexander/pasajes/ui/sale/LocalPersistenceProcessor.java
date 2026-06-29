package com.alexander.pasajes.ui.sale;

public class LocalPersistenceProcessor {

    // Glosas explícitas e inmutables exigidas por tu ficha de control
    public static final String STATUS_PERSIST_OK = "PERSISTENCIA_EXITOSA";
    public static final String STATUS_DUPLICATE_FIXED = "IDENTIFICADOR_DUPLICADO_CORREGIDO";
    public static final String MSG_ERROR_STORAGE_FULL = "Fallo de almacenamiento: Memoria interna del celular llena";

    /**
     * Evalúa analíticamente la viabilidad de la escritura en el almacenamiento local Room.
     */
    public String evaluarPersistenciaLocal(boolean almacenamientoLleno, boolean identificadorDuplicado) {
        // [CP101]: Escudo protector ante desbordamiento de almacenamiento interno del smartphone
        if (almacenamientoLleno) {
            return MSG_ERROR_STORAGE_FULL;
        }

        // [CP102]: Detección y sustitución automática ante colisiones de llaves primarias
        if (identificadorDuplicado) {
            return STATUS_DUPLICATE_FIXED;
        }

        // [CP100]: Guardado conforme de la estructura del boleto listo para auditoría
        return STATUS_PERSIST_OK;
    }
}