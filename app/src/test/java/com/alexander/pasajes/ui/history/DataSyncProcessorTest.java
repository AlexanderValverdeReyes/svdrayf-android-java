package com.alexander.pasajes.ui.history;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class DataSyncProcessorTest {

    private DataSyncProcessor processor;

    @Before
    public void setUp() {
        // Inicialización del motor analítico de sincronización incremental
        processor = new DataSyncProcessor();
    }

    // CP124: ESCENARIO EXITOSO — SUBIDA MASIVA DE REGISTROS (Happy Path)
    @Test
    public void cp124_debeRetornarExitoSiElCelularTieneInternetYVentasPendientes() {
        // 1. ARRANGE (Preparar: Terminal online en Chilca con 12 boletos por subir)
        boolean tieneInternet = true;
        int boletosPendientes = 12;
        boolean corteSenalMedioProceso = false;

        // 2. ACT (Actuar: Solicitar la evaluación de volcado de datos)
        String resultado = processor.evaluarSincronizacion(tieneInternet, boletosPendientes, corteSenalMedioProceso);

        // 3. ASSERT (Verificar: Debe confirmar la subida de datos masiva exitosa)
        assertEquals(DataSyncProcessor.STATUS_SYNC_OK, resultado);
    }

    // CP125: ERROR — CORTE DE SEÑAL DE INTERNET DURANTE EL ENVÍO (Sad Path)
    @Test
    public void cp125_debeHaltarCargaYResguardarPasajesSiLaSenalCaeAMedioEnvio() {
        // 1. ARRANGE (Preparar: Red inestable o caída del socket inalámbrico en ruta)
        boolean tieneInternet = true;
        int boletosPendientes = 5;
        boolean corteSenalMedioProceso = true; // El radiofrecuencia se interrumpe

        // 2. ACT (Actuar: Procesar la contingencia de transporte de red)
        String resultado = processor.evaluarSincronizacion(tieneInternet, boletosPendientes, corteSenalMedioProceso);

        // 3. ASSERT (Verificar: Debe detener la subida inyectando la advertencia del Excel)
        assertEquals(DataSyncProcessor.MSG_ERROR_SIGNAL_DROP, resultado);
    }

    // CP126: ERROR — INTENTO DE SINCRONIZACIÓN SIN DATOS PENDIENTES (Sad Path)
    @Test
    public void cp126_debeCancelarEnvioSiLaColaDeRoomNoRegistraPasajesNuevos() {
        // 1. ARRANGE (Preparar: Todos los boletos del viaje ya se encuentran en sincronizado = true)
        boolean tieneInternet = true;
        int boletosPendientes = 0;
        boolean corteSenalMedioProceso = false;

        // 2. ACT (Actuar: Cobrador presiona repetidamente el botón de subida por precaución)
        String resultado = processor.evaluarSincronizacion(tieneInternet, boletosPendientes, corteSenalMedioProceso);

        // 3. ASSERT (Verificar: Debe cancelar de forma limpia con la glosa de aviso requerida)
        assertEquals(DataSyncProcessor.MSG_INFO_NO_PENDING, resultado);
    }
}