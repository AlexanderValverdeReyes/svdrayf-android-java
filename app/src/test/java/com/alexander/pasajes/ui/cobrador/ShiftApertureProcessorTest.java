package com.alexander.pasajes.ui.cobrador;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class ShiftApertureProcessorTest {

    private ShiftApertureProcessor processor;

    @Before
    public void setUp() {
        // Inicialización de la máquina analítica de apertura de viajes
        processor = new ShiftApertureProcessor();
    }

    // CP72: ESCENARIO EXITOSO — APERTURA CORRECTA DE JORNADA (Happy Path)
    @Test
    public void cp72_debeAutorizarAperturaDeTurnoConParametrosValidos() {
        // 1. ARRANGE (Preparar: Cobrador libre y bus disponible sin asignaciones en la central)
        boolean tieneTurnoLocalPendiente = false;
        boolean esConflictoServidor = false;

        // 2. ACT (Actuar: Presionar el botón "Iniciar Turno de Viaje")
        String resultado = processor.evaluarAperturaTurno(tieneTurnoLocalPendiente, esConflictoServidor);

        // 3. ASSERT (Verificar: Debe estampar el timestamp y permitir emitir pasajes)
        assertEquals(ShiftApertureProcessor.STATUS_APERTURE_OK, resultado);
    }

    // CP73: EXISTENCIA DE UN TURNO PREVIO ABIERTO O INCONCLUSO (Sad Path)
    @Test
    public void cp73_debeBloquearAperturaSiElOperadorTieneUnViajeSinLiquidar() {
        // 1. ARRANGE (Preparar: SQLite registra una jornada del día anterior abierta)
        boolean tieneTurnoLocalPendiente = true;
        boolean esConflictoServidor = false;

        // 2. ACT (Actuar: Intentar forzar una nueva apertura de viaje)
        String resultado = processor.evaluarAperturaTurno(tieneTurnoLocalPendiente, esConflictoServidor);

        // 3. ASSERT (Verificar: Debe congelar la acción y reencauzar al formulario anterior)
        assertEquals(ShiftApertureProcessor.MSG_ERROR_PREVIOUS_OPEN, resultado);
    }

    // CP74: UNIDAD VEHICULAR RETENIDA O OCUPADA EN CLOUD (Sad Path)
    @Test
    public void cp74_debeBloquearAperturaLocalSiElServidorDevuelveConflictoCuatrosCientosNueve() {
        // 1. ARRANGE (Preparar: Servidor reporta que el bus ya está operando con otra tripulación)
        boolean tieneTurnoLocalPendiente = false;
        boolean esConflictoServidor = true;

        // 2. ACT (Actuar: Enviar la trama de apertura a PostgreSQL a través del API)
        String resultado = processor.evaluarAperturaTurno(tieneTurnoLocalPendiente, esConflictoServidor);

        // 3. ASSERT (Verificar: Debe desplegar el aviso restrictivo de liberación administrativa)
        assertEquals(ShiftApertureProcessor.MSG_ERROR_CLOUD_BLOCKED, resultado);
    }
}