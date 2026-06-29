package com.alexander.pasajes.ui.inspector;

public class QrAuthenticityProcessor {
    public static final String STATUS_AUTHENTIC_OK = "BOLETO_AUTENTICO_CONFORME";
    public static final String MSG_ERROR_NOT_RECONOCIDO = "Error: Código no reconocido";
    public static final String MSG_ERROR_DUPLICADO = "Error: Pasaje ya verificado";

    /**
     * Evalúa analíticamente la legitimidad criptográfica y la unicidad del pasaje escaneado.
     * @param existeEnSistema Indica si el hash se encuentra indexado en la nube Neon DB.
     * @param yaEscaneadoEnBus Indica si el fiscalizador ya procesó esta firma en esta misma revisión.
     */
    public String evaluarAutenticidadQr(boolean existeEnSistema, boolean yaEscaneadoEnBus) {
        // [CP120]: Intento de fraude con códigos QR falsos o ajenos al ecosistema corporativo
        if (!existeEnSistema) {
            return MSG_ERROR_NOT_RECONOCIDO;
        }

        // [CP121]: Control anti-duplicidad de pasajes (Pasajero que presta su ticket ya verificado)
        if (yaEscaneadoEnBus) {
            return MSG_ERROR_DUPLICADO;
        }

        // [CP119]: Confirmación de boleto original, vigente y por primera vez escaneado
        return STATUS_AUTHENTIC_OK;
    }
}