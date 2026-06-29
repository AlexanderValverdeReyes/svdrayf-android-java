package com.alexander.pasajes.ui.cobrador; // 🟢 Paquete oficial y unificado de la capa de cobrador

public class RouteSelectionProcessor {

    // Glosas corporativas exactas y obligatorias extraídas de tu ficha de control
    public static final String STATUS_ROUTE_OK = "CONFIGURACION_RUTA_CONFORME";
    public static final String MSG_ERROR_MIDWAY_CHANGE = "Operación denegada por seguridad: No se puede modificar el tipo de ruta una vez iniciado el cobro de pasajes. Para cambiar de modalidad debe cerrar y liquidar contablemente el viaje en curso";
    public static final String MSG_ERROR_CORRUPTED_FARES = "Fallo de configuración: Estructura de paraderos ilegible de forma local. Inicialice el viaje en modalidad Directo o conéctese a internet en la central para reaprovisionar el tarifario estático";

    /**
     * Valida las restricciones de seguridad contable y consistencia física de tarifas por tramos.
     */
    public String evaluarConfiguracionRuta(boolean tieneTransaccionesPrevias, boolean tarifarioIncompleto, String modalidadSeleccionada) {
        // [CP70]: Candado de seguridad para evitar fraudes tributarios a mitad de la carretera
        if (tieneTransaccionesPrevias) {
            return MSG_ERROR_MIDWAY_CHANGE;
        }

        // [CP71]: Escudo protector ante caídas o corrupciones de sincronización en Room/SQLite
        if ("PARADEROS".equals(modalidadSeleccionada) && tarifarioIncompleto) {
            return MSG_ERROR_CORRUPTED_FARES;
        }

        // [CP69]: Configuración idónea en terminal lista para iniciar boletaje
        return STATUS_ROUTE_OK;
    }
}