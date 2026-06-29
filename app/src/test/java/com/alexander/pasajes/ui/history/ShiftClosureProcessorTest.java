package com.alexander.pasajes.ui.history;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class ShiftClosureProcessorTest {

    private ShiftClosureProcessor processor;

    @Before
    public void setUp() {
        // Inicialización global del procesador analítico de cierre de jornadas
        processor = new ShiftClosureProcessor();
    }

    // CP94: ESCENARIO EXITOSO — CIERRE CORRECTO DE VIAJE (Happy Path)
    @Test
    public void cp94_debePermitirCierreDeTurnoSiExisteInternetYLaTicketeraEstaSana() {
        // 1. ARRANGE (Preparar: Bus llegó a destino final con conectividad online activa)
        boolean tieneInternet = true;
        boolean errorMecanicoImpresora = false;
        boolean esIntentoReimpresion = false;

        // 2. ACT (Actuar: Procesar la solicitud del control "Cerrar Turno")
        String resultado = processor.evaluarEstadoCierre(tieneInternet, errorMecanicoImpresora, esIntentoReimpresion);

        // 3. ASSERT (Verificar: Debe consolidar las ventas y subir el arqueo a Neon DB)
        assertEquals(ShiftClosureProcessor.STATUS_SUCCESS_ONLINE, resultado);
    }

    // CP95: VENTAS LOCALES PENDIENTES — SIN SEÑAL EN CARRETERA (Sad Path)
    @Test
    public void cp95_debeRedireccionarAlLoginManteniendoElTurnoAbiertoSiNoHayInternet() {
        // 1. ARRANGE (Preparar: Intento de cierre en zona muerta de señal en la Panamericana)
        boolean tieneInternet = false;
        boolean errorMecanicoImpresora = false;
        boolean esIntentoReimpresion = false;

        // 2. ACT (Actuar: Evaluar las restricciones arquitectónicas de red)
        String resultado = processor.evaluarEstadoCierre(tieneInternet, errorMecanicoImpresora, esIntentoReimpresion);

        // 3. ASSERT (Verificar: Debe rechazar el vaciado cloud y forzar la redirección temporal)
        assertEquals(ShiftClosureProcessor.STATUS_OFFLINE_REDIRECT, resultado);
    }

    // CP96: REIMPRESIÓN POR FALLA MECÁNICA O FALTA DE PAPEL (Sad Path / Contingencia)
    @Test
    public void cp96_debeHabilitarLaReimpresionDelArqueoSinDuplicarMontosDeCaja() {
        // 1. ARRANGE (Preparar: Ticketera se apaga o se queda sin papel durante el reporte de cierre)
        boolean tieneInternet = true;
        boolean errorMecanicoImpresora = true;
        boolean esIntentoReimpresion = true;

        // 2. ACT (Actuar: El cobrador presiona el botón de recuperación "Reimprimir Cierre")
        String resultado = processor.evaluarEstadoCierre(tieneInternet, errorMecanicoImpresora, esIntentoReimpresion);

        // 3. ASSERT (Verificar: El sistema debe habilitar el reenvío físico seguro sin alterar Room)
        assertEquals(ShiftClosureProcessor.STATUS_REPRINT_ALLOWED, resultado);
    }
}