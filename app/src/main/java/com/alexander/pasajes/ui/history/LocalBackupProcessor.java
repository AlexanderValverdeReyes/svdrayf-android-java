package com.alexander.pasajes.ui.history;

public class LocalBackupProcessor {

    public static final String STATUS_BACKUP_OK = "EXPORTACION_RESPALDO_INICIADA";
    public static final String MSG_ERROR_NETWORK_INTERRUPTED = "Error de red: Exportación interrumpida";
    public static final String MSG_ERROR_DATABASE_EMPTY = "Operación cancelada: La base de datos local se encuentra vacía";

    /**
     * Evalúa analíticamente la viabilidad de empaquetar y transferir el archivo SQLite relacional.
     * @param tieneInternet Conectividad móvil actual disponible.
     * @param cantidadVentasRegistradas Número de boletos almacenados localmente en la jornada actual.
     * @param corteSenalMedioEnvio Flag de contingencia que captura caídas de sockets HTTP durante el streaming del archivo.
     */
    public String evaluarExportacionBackup(boolean tieneInternet, int cantidadVentasRegistradas, boolean corteSenalMedioEnvio) {
        // [CP141]: Intento de exportar una base de datos sin operaciones (Nueva instalación o limpia)
        if (cantidadVentasRegistradas == 0) {
            return MSG_ERROR_DATABASE_EMPTY;
        }

        // [CP140]: Caída del internet a mitad de la transferencia del archivo relacional completo
        if (!tieneInternet || corteSenalMedioEnvio) {
            return MSG_ERROR_NETWORK_INTERRUPTED;
        }

        // [CP139]: Escenario Exitoso — Envío conforme del clon binario hacia Neon DB
        return STATUS_BACKUP_OK;
    }
}