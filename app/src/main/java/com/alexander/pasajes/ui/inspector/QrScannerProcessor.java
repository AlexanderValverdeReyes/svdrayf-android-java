package com.alexander.pasajes.ui.inspector;
public class QrScannerProcessor {

    public static final String STATUS_SCAN_SUCCESS = "SCAN_LECTURA_EXITOSA";
    public static final String MSG_ERROR_ILEGIBLE = "QR ilegible. Ingrese el número de serie manual";
    public static final String MSG_ERROR_PERMISOS = "Acceso denegado. Habilite los permisos de cámara en los ajustes";

    /**
     * Evalúa la viabilidad del flujo físico de captura de imágenes por hardware.
     */
    public String evaluarEscaneoQr(boolean tienePermisoCamara, boolean escaneoCompletado, int tiempoEsperaSegundos) {
        // [CP112]: Interceptación perimetral si el sistema operativo no tiene la autorización
        if (!tienePermisoCamara) {
            return MSG_ERROR_PERMISOS;
        }

        // [CP111]: El código supera los 3 segundos de espera sin leer por estar roto o borroso
        if (!escaneoCompletado && tiempoEsperaSegundos >= 3) {
            return MSG_ERROR_ILEGIBLE;
        }

        // [CP110]: Lectura conforme, decodifica el Hash de Neon DB al instante
        return STATUS_SCAN_SUCCESS;
    }
}