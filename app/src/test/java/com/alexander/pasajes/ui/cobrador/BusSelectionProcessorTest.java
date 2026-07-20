package com.alexander.pasajes.ui.cobrador;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class BusSelectionProcessorTest {

    private BusSelectionProcessor processor;

    @Before
    public void setUp() {
        // Inicialización global del procesador de asignación vehicular
        processor = new BusSelectionProcessor();
    }

    // CP65: ESCENARIO EXITOSO — VINCULACIÓN CORRECTA DE BUS AL TURNO (Happy Path)
    @Test
    public void cp65_debeAprobarVinculacionSiElBusExisteYEstaDisponible() {
        // 1. ARRANGE (El catálogo SQLite móvil tiene el bus, está libre y se encuentra activo)
        boolean busVisibleEnLocal = true;
        boolean tieneInternet = true;
        boolean yaRegistraTurnoActivo = false;
        boolean esBusActivo = true;

        // 2. ACT (Simular la asignación del vehículo conforme)
        String resultado = processor.evaluarSeleccionBus(busVisibleEnLocal, tieneInternet, yaRegistraTurnoActivo, esBusActivo);

        // 3. ASSERT (Debe destrabar la transición hacia el catálogo de rutas)
        assertEquals(BusSelectionProcessor.STATUS_LINK_OK, resultado);
    }

    // CP66: UNIDAD VEHICULAR NUEVA - SINCRONIZACIÓN CACHÉ (Happy Path)
    @Test
    public void cp66_debePermitirSincronizarEInyectarBusNuevoSiHayAccesoAInternet() {
        // 1. ARRANGE (Autobús recién ingresado que no figura en la caché local del smartphone)
        boolean busVisibleEnLocal = false;
        boolean tieneInternet = true;
        boolean yaRegistraTurnoActivo = false;
        boolean esBusActivo = true;

        // 2. ACT (Procesar la descarga de maestros en segundo plano)
        String resultado = processor.evaluarSeleccionBus(busVisibleEnLocal, tieneInternet, yaRegistraTurnoActivo, esBusActivo);

        // 3. ASSERT (Debe autorizar la actualización de la tabla SQLite local)
        assertEquals(BusSelectionProcessor.STATUS_SYNC_OK, resultado);
    }

    // CP67: ERROR - UNIDAD DE BUS OCUPADA O EN PARALELO (Sad Path)
    @Test
    public void cp67_debeBloquearAsignacionYMostrarMensajeExplicitoSiLaUnidadEstaEnTransito() {
        // 1. ARRANGE (Otro cobrador mantiene una jornada sin liquidar en el mismo vehículo)
        boolean busVisibleEnLocal = true;
        boolean tieneInternet = true;
        boolean yaRegistraTurnoActivo = true;
        boolean esBusActivo = true;

        // 2. ACT (Forzar intento de enlace de la placa duplicada)
        String resultado = processor.evaluarSeleccionBus(busVisibleEnLocal, tieneInternet, yaRegistraTurnoActivo, esBusActivo);

        // 3. ASSERT (Debe interceptar la colisión y desplegar la glosa anti-fraude exacta)
        assertEquals(BusSelectionProcessor.MSG_ERROR_BUS_OCCUPIED, resultado);
    }

    // NUEVO TEST DE COBERTURA: ERROR - UNIDAD VEHICULAR DADA DE BAJA (Sad Path)
    @Test
    public void debeBloquearAsignacionYMostrarMensajeDeBajaSiElBusEstaInactivo() {
        // 1. ARRANGE (El bus existe en local pero la columna estado en la base de datos es false)
        boolean busVisibleEnLocal = true;
        boolean tieneInternet = true;
        boolean yaRegistraTurnoActivo = false;
        boolean esBusActivo = false;

        // 2. ACT (Intentar abrir turno con una unidad deshabilitada administrativamente)
        String resultado = processor.evaluarSeleccionBus(busVisibleEnLocal, tieneInternet, yaRegistraTurnoActivo, esBusActivo);

        // 3. ASSERT (Debe interceptar la inactividad y retornar el mensaje de bloqueo de jornada)
        assertEquals(BusSelectionProcessor.MSG_ERROR_BUS_INACTIVE, resultado);
    }
}