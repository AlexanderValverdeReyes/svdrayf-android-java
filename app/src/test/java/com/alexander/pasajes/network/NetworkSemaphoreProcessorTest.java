package com.alexander.pasajes.network;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class NetworkSemaphoreProcessorTest {

    private NetworkSemaphoreProcessor processor;

    @Before
    public void setUp() {
        // Inicialización del motor analítico de semáforo de conectividad
        processor = new NetworkSemaphoreProcessor();
    }

    // CP129: ESCENARIO EXITOSO — CAMBIO CORRECTO A VERDE (Happy Path)
    @Test
    public void cp129_debeMostrarIconoVerdeSiTieneInternetYNoHayDatosPendientes() {
        // 1. ARRANGE (Preparar: Dispositivo online y Room totalmente sincronizado)
        boolean tieneInternet = true;
        int boletosPendientes = 0;

        // 2. ACT (Actuar: Evaluar de forma automática el estado de la conexión)
        String resultado = processor.evaluarEstadoRed(tieneInternet, boletosPendientes);

        // 3. ASSERT (Verificar: Debe fijar el indicador en verde conforme)
        assertEquals(NetworkSemaphoreProcessor.COLOR_VERDE, resultado);
    }

    // CP129: ESCENARIO EXITOSO — CAMBIO CORRECTO A NARANJA (Happy Path)
    @Test
    public void cp129_debeMostrarIconoNaranjaSiTieneInternetPeroHayVentasLocalesSinSubir() {
        // 1. ARRANGE (Preparar: Hay señal en paradero pero quedan pasajes guardados en cola)
        boolean tieneInternet = true;
        int boletosPendientes = 8;

        // 2. ACT (Actuar: Procesar la combinación de infraestructura)
        String resultado = processor.evaluarEstadoRed(tieneInternet, boletosPendientes);

        // 3. ASSERT (Verificar: Debe cambiar a naranja alertando sincronización parcial)
        assertEquals(NetworkSemaphoreProcessor.COLOR_NARANJA, resultado);
    }

    // CP130: DESCONEXIÓN TOTAL DE DATOS MÓVILES (Sad Path / Offline)
    @Test
    public void cp130_debeCambiarInmediatamenteARojoSiElCelularPierdeElAccesoAItermet() {
        // 1. ARRANGE (Preparar: El bus ingresa a una zona muerta en la carretera Mala-Lima)
        boolean tieneInternet = false;
        int boletosPendientes = 4; // Los registros quedan resguardados intactos

        // 2. ACT (Actuar: El sistema detecta el cambio en las antenas del smartphone)
        String resultado = processor.evaluarEstadoRed(tieneInternet, boletosPendientes);

        // 3. ASSERT (Verificar: Debe teñir la cabecera de rojo indicando estado Offline inmediato)
        assertEquals(NetworkSemaphoreProcessor.COLOR_ROJO, resultado);
    }
}