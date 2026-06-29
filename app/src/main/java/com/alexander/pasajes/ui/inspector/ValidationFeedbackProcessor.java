package com.alexander.pasajes.ui.inspector;
public class ValidationFeedbackProcessor {

    // Constantes analíticas cromáticas y de error para la UI del fiscalizador
    public static final String COLOR_VERDE_CONFORME = "UI_PANTALLA_COMPLETA_VERDE";
    public static final String COLOR_ROJO_ERROR_ESTADO = "UI_PANTALLA_COMPLETA_ROJA_DETALLES";
    public static final String MSG_ERROR_NO_RECONOCIDO = "Error: Código no reconocido";

    /**
     * Evalúa la respuesta transaccional del servidor para determinar el feedback visual y acústico.
     */
    public String evaluarFeedbackVisual(boolean encontradoEnCentral, String estadoBoleto, String estadoTurno) {
        // [CP114]: Código QR inválido que no pertenece al ecosistema de base de datos Neon DB
        if (!encontradoEnCentral) {
            return MSG_ERROR_NO_RECONOCIDO;
        }

        // [CP113]: Escenario Exitoso — Registro emparejado, vigente y con jornada abierta
        if ("VALIDO".equals(estadoBoleto) && "ABIERTO".equals(estadoTurno)) {
            return COLOR_VERDE_CONFORME;
        }

        // Caso de Contingencia: El boleto existe pero acarrea alertas de anulación o turnos pasados
        return COLOR_ROJO_ERROR_ESTADO;
    }
}