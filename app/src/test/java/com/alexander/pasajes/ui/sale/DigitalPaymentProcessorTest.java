package com.alexander.pasajes.ui.sale;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class DigitalPaymentProcessorTest {

    private DigitalPaymentProcessor processor;

    @Before
    public void setUp() {
        // Inicialización del motor analítico de validación de preventa digital
        processor = new DigitalPaymentProcessor();
    }

    // CP81: ESCENARIO EXITOSO — DESPLIEGUE CORRECTO DE CÓDIGO QR (Happy Path)
    @Test
    public void cp81_debePermitirProyeccionDeQRSielArchivoFisicoEstaDisponible() {
        // 1. ARRANGE (Preparar: Solicitud de billetera digital con recurso verificado en disco)
        String metodoSeleccionado = "QR";
        boolean qrDisponibleEnMemoria = true;
        boolean cambioUltimoMomento = false;

        // 2. ACT (Actuar: Procesar la conmutación de canal de recaudación)
        String resultado = processor.evaluarModalidadPago(metodoSeleccionado, qrDisponibleEnMemoria, cambioUltimoMomento);

        // 3. ASSERT (Verificar: Debe aprobar instantáneamente la proyección a pantalla completa)
        assertEquals(DigitalPaymentProcessor.STATUS_QR_DISPLAYED, resultado);
    }

    // CP82: ALTERNANCIA POR CAMBIO DE PARECER DEL PASAJERO (Happy Path)
    @Test
    public void cp82_debeOcultarQRYSetearBanderaDeAuditoriaSiConmutaAEfectivo() {
        // 1. ARRANGE (Preparar: Pasajero visualiza el QR pero decide pagar con monedas a último minuto)
        String metodoSeleccionado = "EFECTIVO";
        boolean qrDisponibleEnMemoria = true;
        boolean cambioUltimoMomento = true;

        // 2. ACT (Actuar: Cobrador presiona el control alterno de efectivo)
        String resultado = processor.evaluarModalidadPago(metodoSeleccionado, qrDisponibleEnMemoria, cambioUltimoMomento);

        // 3. ASSERT (Verificar: Debe retornar el estado conforme para estampar huboIntentoQR = true)
        assertEquals(DigitalPaymentProcessor.STATUS_CASH_CONMUTED, resultado);
    }

    // CP83: IMAGEN DEL CÓDIGO QR ESTÁTICO DAÑADA O AUSENTE (Sad Path)
    @Test
    public void cp83_debeBloquearMundoDigitalYForzarEfectivoSiFaltaElRecursoLocal() {
        // 1. ARRANGE (Preparar: Intento de cobro QR con el archivo ausente en la memoria del celular)
        String metodoSeleccionado = "QR";
        boolean qrDisponibleEnMemoria = false;
        boolean cambioUltimoMomento = false;

        // 2. ACT (Actuar: Interceptación perimetral del hardware)
        String resultado = processor.evaluarModalidadPago(metodoSeleccionado, qrDisponibleEnMemoria, cambioUltimoMomento);

        // 3. ASSERT (Verificar: Debe suspender temporalmente el módulo y exigir la glosa de contingencia)
        assertEquals(DigitalPaymentProcessor.MSG_ERROR_QR_MISSING, resultado);
    }
}