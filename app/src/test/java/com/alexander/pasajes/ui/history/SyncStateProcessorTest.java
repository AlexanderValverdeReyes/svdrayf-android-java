package com.alexander.pasajes.ui.history;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class SyncStateProcessorTest {

    private SyncStateProcessor processor;

    @Before
    public void setUp() {
        // Inicialización del motor analítico de estados de red
        processor = new SyncStateProcessor();
    }

    // CP127: ESCENARIO EXITOSO — PROGRESO DE CARGA CONFORME (Happy Path)
    @Test
    public void cp127_debeInformarProgresoActivoSiElCanalEstaTransmitiendoConInternet() {
        // 1. ARRANGE (Preparar: Sistema enviando el paquete de boletos con internet estable)
        boolean estaTransmitiendo = true;
        boolean tieneInternet = true;
        boolean cargaCompletada = false;

        // 2. ACT (Actuar: Solicitar al procesador el estado dinámico del avance)
        String resultado = processor.evaluarEstadoSync(estaTransmitiendo, tieneInternet, cargaCompletada);

        // 3. ASSERT (Verificar: Debe retornar el estado de transmisión activo para la UI)
        assertEquals(SyncStateProcessor.STATUS_PROGRESS_ACTIVE, resultado);
    }

    // CP128: ERROR — DESCONEXIÓN DE INTERNET EN PLENA CARGA (Sad Path)
    @Test
    public void cp128_debePausarSincronizacionYNotificarFaltaDeSenalSiSeCortaElInternet() {
        // 1. ARRANGE (Preparar: Red del smartphone se cae por completo en medio del envío de datos)
        boolean estaTransmitiendo = true;
        boolean tieneInternet = false; // Caída del enlace inalámbrico
        boolean cargaCompletada = false;

        // 2. ACT (Actuar: Capturar la latencia o corte de enlace)
        String resultado = processor.evaluarEstadoSync(estaTransmitiendo, tieneInternet, cargaCompletada);

        // 3. ASSERT (Verificar: Debe congelar el avance y disparar la alerta visual mandatoria del Excel)
        assertEquals(SyncStateProcessor.MSG_ERROR_SYNC_PAUSED, resultado);
    }
}