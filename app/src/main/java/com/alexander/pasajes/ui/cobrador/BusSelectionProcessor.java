package com.alexander.pasajes.ui.cobrador;

public class BusSelectionProcessor {

    public static final String STATUS_LINK_OK = "VINCULACION_EXITOSA";
    public static final String STATUS_SYNC_OK = "CATALOGO_SINCRONIZADO";
    public static final String MSG_ERROR_BUS_OCCUPIED = "Acción Denegada: El vehículo seleccionado ya registra un turno de viaje abierto en tránsito. El cobrador previo debe cerrar y liquidar su jornada web para liberar la unidad.";
    public static final String MSG_ERROR_OFFLINE_MISSING = "Error de contingencia: El vehículo no se encuentra en la base local y no hay acceso a internet para sincronizar.";
    public static final String MSG_ERROR_BUS_INACTIVE = "Bloqueo de Jornada: El autobús seleccionado ha sido dado de baja por la administración. No se permiten operaciones.";

    /**
     * Evalúa la disponibilidad operativa del autobús y el estado de la caché SQLite.
     */
    public String evaluarSeleccionBus(boolean busVisibleEnLocal, boolean tieneInternet, boolean yaRegistraTurnoActivo, boolean esBusActivo) {
        // Regla de negocio: Restricción para vehículos inactivos o dados de baja
        if (busVisibleEnLocal && !esBusActivo) {
            return MSG_ERROR_BUS_INACTIVE;
        }

        // [CP67]: Regla de asignación en paralelo o sesión huérfana de otra unidad
        if (yaRegistraTurnoActivo) {
            return MSG_ERROR_BUS_OCCUPIED;
        }

        // [CP66]: Unidad nueva en la flota que no se encuentra en la caché del smartphone
        if (!busVisibleEnLocal) {
            if (tieneInternet) {
                return STATUS_SYNC_OK;
            } else {
                return MSG_ERROR_OFFLINE_MISSING;
            }
        }

        // [CP65]: Flujo conforme, catálogo cargado y unidad libre para operar
        return STATUS_LINK_OK;
    }
}