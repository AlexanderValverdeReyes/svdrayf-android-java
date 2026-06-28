package com.alexander.pasajes.ui.printer; // 🟢 Paquete oficial del módulo de impresión

public class PrinterConnectivityProcessor {

    // Constantes globales con las alertas explícitas exigidas por la ficha de control
    public static final String MSG_SUCCESS_CONNECTED = "CONNECTED_SUCCESS";
    public static final String MSG_WARN_NOT_FOUND = "Búsqueda finalizada: Miniticketera térmica no encontrada en ruta. Verifique que el periférico esté encendido.";
    public static final String MSG_ERROR_BT_DISABLED = "Acción bloqueada: El Bluetooth del smartphone se encuentra apagado. Active la casilla de Bluetooth desde la barra de notificaciones de su celular para poder enlazar la ticketera térmica";
    public static final String MSG_ERROR_TIMEOUT = "Fallo de enlace: No se pudo completar el emparejamiento con la ticketera. Apague y encienda la impresora térmica y reintente el proceso";

    /**
     * Procesa analíticamente los estados del hardware de radiofrecuencia y sockets seriales.
     */
    public String evaluarEstadoConexion(boolean bluetoothActivado, boolean escaneoCompletado, boolean dispositivoEncontrado, boolean conexionExitosa, int tiempoEsperaSegundos) {
        if (!bluetoothActivado) {
            return MSG_ERROR_BT_DISABLED; // CP60
        }
        if (escaneoCompletado && !dispositivoEncontrado) {
            return MSG_WARN_NOT_FOUND; // CP59
        }
        if (!conexionExitosa && tiempoEsperaSegundos >= 5) {
            return MSG_ERROR_TIMEOUT; // CP61
        }
        if (bluetoothActivado && dispositivoEncontrado && conexionExitosa) {
            return MSG_SUCCESS_CONNECTED; // CP58
        }
        return "ESTADO_INCOMPLETO";
    }
}