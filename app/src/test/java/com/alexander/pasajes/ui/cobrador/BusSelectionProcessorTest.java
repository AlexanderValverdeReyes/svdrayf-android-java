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
        // 1. ARRANGE (Preparar: El catálogo SQLite móvil tiene guardado el bus y la unidad está libre)
        boolean busVisibleEnLocal = true;
        boolean tieneInternet = true;
        boolean yaRegistraTurnoActivo = false;

        // 2. ACT (Actuar: Simular la pulsación táctil del control "Confirmar unidad")
        String resultado = processor.evaluarSeleccionBus(busVisibleEnLocal, tieneInternet, yaRegistraTurnoActivo);

        // 3. ASSERT (Verificar: Debe destrabar la transición hacia el catálogo de rutas)
        assertEquals(BusSelectionProcessor.STATUS_LINK_OK, resultado);
    }

    // CP66: UNIDAD VEHICULAR NUEVA - SINCRONIZACIÓN CACHÉ (Happy Path)
    @Test
    public void cp66_debePermitirSincronizarEInyectarBusNuevoSiHayAccesoAInternet() {
        // 1. ARRANGE (Preparar: Autobús recién ingresado a la ruta Mala-Lima que no figura en el smartphone)
        boolean busVisibleEnLocal = false;
        boolean tieneInternet = true;
        boolean yaRegistraTurnoActivo = false;

        // 2. ACT (Actuar: Procesar la descarga de maestros en milisegundos)
        String resultado = processor.evaluarSeleccionBus(busVisibleEnLocal, tieneInternet, yaRegistraTurnoActivo);

        // 3. ASSERT (Verificar: Debe autorizar la actualización de la tabla SQLite local)
        assertEquals(BusSelectionProcessor.STATUS_SYNC_OK, resultado);
    }

    // CP67: ERROR - UNIDAD DE BUS OCUPADA O EN PARALELO (Sad Path)
    @Test
    public void cp67_debeBloquearAsignacionYMostrarMensajeExplicitoSiLaUnidadEstaEnTransito() {
        // 1. ARRANGE (Preparar: Otro cobrador mantiene una jornada sin liquidar en el mismo vehículo)
        boolean busVisibleEnLocal = true;
        boolean tieneInternet = true;
        boolean yaRegistraTurnoActivo = true;

        // 2. ACT (Actuar: Forzar intento de enlace de la placa duplicada)
        String resultado = processor.evaluarSeleccionBus(busVisibleEnLocal, tieneInternet, yaRegistraTurnoActivo);

        // 3. ASSERT (Verificar: Debe interceptar la colisión y desplegar la glosa anti-fraude exacta)
        assertEquals(BusSelectionProcessor.MSG_ERROR_BUS_OCCUPIED, resultado);
    }
}