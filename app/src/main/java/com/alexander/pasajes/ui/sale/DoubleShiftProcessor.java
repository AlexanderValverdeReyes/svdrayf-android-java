package com.alexander.pasajes.ui.sale;

public class DoubleShiftProcessor {

    // Constantes de control y glosas exactas extraídas de tu matriz Excel
    public static final String STATUS_SHIFT_OK = "SHIFT_CONFORME";
    public static final String MSG_ERROR_HARDWARE_SHUTDOWN = "Detección de falla previa: El viaje anterior no se cerró correctamente por apagado de hardware. Por favor, complete el cierre pendiente.";
    public static final String MSG_ERROR_DOUBLE_LOGIN = "Acción Bloqueada: El bus o cobrador ya cuenta con un turno en estado abierto registrado en la central web desde otro dispositivo.";

    /**
     * Evalúa la viabilidad de la sesión de trabajo y la concurrencia de terminales móviles.
     */
    public String evaluarDobleTurno(boolean tieneTurnoActivoServidor, boolean sufrioApagadoImprevisto) {
        // [CP104]: El sistema detecta que la sesión previa se interrumpió abruptamente por falta de batería
        if (sufrioApagadoImprevisto) {
            return MSG_ERROR_HARDWARE_SHUTDOWN;
        }

        // [CP105]: Interceptación de concurrencia si se intenta abrir el mismo turno en un segundo celular
        if (tieneTurnoActivoServidor) {
            return MSG_ERROR_DOUBLE_LOGIN;
        }

        // [CP103]: Apertura correcta de jornada libre de colisiones
        return STATUS_SHIFT_OK;
    }
}