package com.alexander.pasajes.network;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class NetworkMonitoringProcessorTest {

    private NetworkMonitoringProcessor processor;

    @Before
    public void setUp() {
        // Inicialización del motor analítico de monitoreo en background
        processor = new NetworkMonitoringProcessor();
    }

    // CP131: ESCENARIO EXITOSO — DETECCIÓN AUTOMÁTICA DE INTERNET (Happy Path)
    @Test
    public void cp131_debeActivarSubidaDeFondoSiRecuperaSeñalYHayRegistrosLocales() {
        // 1. ARRANGE (Preparar: Señal restaurada estable con 5 pasajes guardados en Room)
        boolean tieneInternet = true;
        int fluctuacionesSenoRecientes = 0;
        int boletosPendientes = 5;

        // 2. ACT (Actuar: Procesar el cambio de antena del smartphone automáticamente)
        String resultado = processor.evaluarMonitoreoConexion(tieneInternet, fluctuacionesSenoRecientes, boletosPendientes);

        // 3. ASSERT (Verificar: Debe iniciar la subida de fondo arrojando la glosa del Excel)
        assertEquals(NetworkMonitoringProcessor.MSG_INFO_CONNECTED, resultado);
    }

    // CP132: ERROR — INESTABILIDAD SEVERA DE LA SEÑAL CELULAR (Sad Path)
    @Test
    public void cp132_debePausarIntentosPorDiezSegundosSiLaRedConectaYDesconectaRepetidamente() {
        // 1. ARRANGE (Preparar: Dispositivo oscila el estado de red repetidas veces en pocos segundos)
        boolean tieneInternet = true;
        int fluctuacionesSenoRecientes = 4; // Servidor central reporta microcortes continuos
        int boletosPendientes = 3;

        // 2. ACT (Actuar: Interceptar el bucle de reconexión cíclico)
        String resultado = processor.evaluarMonitoreoConexion(tieneInternet, fluctuacionesSenoRecientes, boletosPendientes);

        // 3. ASSERT (Verificar: El sistema debe congelar las llamadas por 10 segundos protegiendo el hardware)
        assertEquals(NetworkMonitoringProcessor.MSG_WARN_UNSTABLE, resultado);
    }
}