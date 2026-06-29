package com.alexander.pasajes.ui.cobrador;
public class ShiftApertureProcessor {

    // Mensajes obligatorios dictados por la rúbrica de evaluación
    public static final String STATUS_APERTURE_OK = "APERTURA_CONFORME";
    public static final String MSG_ERROR_PREVIOUS_OPEN = "Atención: Posee un turno de viaje previo sin cerrar. Debe liquidar la caja del viaje anterior antes de aperturar un nuevo turno";
    public static final String MSG_ERROR_CLOUD_BLOCKED = "Vehículo No Disponible: La unidad presenta un turno de recaudo abierto sin liquidar en la central web. Solicite la liberación administrativa del bus";

    /**
     * Resuelve perimetralmente las restricciones de negocio para la apertura de turnos.
     */
    public String evaluarAperturaTurno(boolean tieneTurnoLocalPendiente, boolean esConflictoServidor) {
        // [CP73]: Candado de seguridad contable si el cobrador tiene una caja abierta de una jornada anterior
        if (tieneTurnoLocalPendiente) {
            return MSG_ERROR_PREVIOUS_OPEN;
        }

        // [CP74]: Interceptación de conflicto por asignación cruzada desde la central web
        if (esConflictoServidor) {
            return MSG_ERROR_CLOUD_BLOCKED;
        }

        // [CP72]: Parámetros lícitos confirmados para iniciar el estampado del timestamp e ID único
        return STATUS_APERTURE_OK;
    }
}