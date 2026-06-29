package com.alexander.pasajes.ui.sale;

public class PassengerTypeProcessor {

    // Alertas y glosas explícitas exigidas en las especificaciones del Excel
    public static final String STATUS_PASSENGER_OK = "PASSENGER_CONFORME";
    public static final String MSG_ERROR_MISSING_STOPS = "Acción requerida: Registre primero los paraderos de tramo del viaje antes de determinar el tipo de pasaje";
    public static final String MSG_WARN_UNIVERISTARIO_HOLIDAY = "Aviso: Pasaje Universitario seleccionado en día no laborable/feriado. Confirme la vigencia del carné físico del estudiante antes de proceder";

    /**
     * Evalúa la consistencia de parámetros para la asignación de categorías de pasajes.
     */
    public String evaluarSeleccionPasajero(int origenId, int destinoId, String tipoPasajero, String regimenDia) {
        // [CP76]: Bloqueo preventivo si el cobrador presiona categorías con paraderos vacíos
        if (origenId == -1 || destinoId == -1) {
            return MSG_ERROR_MISSING_STOPS;
        }

        // [CP77]: Alerta preventiva flotante al aplicar tarifa Universitaria en días no laborables o feriados
        if ("Universitario".equals(tipoPasajero) && "FERIADO".equals(regimenDia)) {
            return MSG_WARN_UNIVERISTARIO_HOLIDAY;
        }

        // [CP75]: Parámetros conformes para proceder a identificar la tarifa estática
        return STATUS_PASSENGER_OK;
    }
}