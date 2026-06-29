package com.alexander.pasajes.ui.history;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class TicketCancellationProcessorTest {

    private TicketCancellationProcessor processor;

    @Before
    public void setUp() {
        // Inicialización del motor analítico de bajas contables
        processor = new TicketCancellationProcessor();
    }

    // CP89: ESCENARIO EXITOSO — ANULACIÓN CORRECTA DE PASAJE (Happy Path)
    @Test
    public void cp89_debePermitirAnulacionLocalSiElBoletoNoHaSidoAuditadoPorInspector() {
        // 1. ARRANGE (Preparar: Boleto activo en el turno actual sin marcas de fiscalización)
        boolean yaAnulado = false;
        boolean fueValidadoPorInspector = false;

        // 2. ACT (Actuar: Solicitar la viabilidad de la baja del comprobante)
        String resultado = processor.evaluarAnulacionBoleto(yaAnulado, fueValidadoPorInspector);

        // 3. ASSERT (Verificar: Debe aprobar la baja descontando el dinero de la caja)
        assertEquals(TicketCancellationProcessor.STATUS_CANCEL_OK, resultado);
    }

    // CP90: ERROR — BOLETO VALIDADO EN RUTA POR INSPECTOR (Sad Path)
    @Test
    public void cp90_debeBloquearOperacionYMostrarMensajeExplicitoSiElBoletoYaFueAuditado() {
        // 1. ARRANGE (Preparar: Fiscalizador escaneó el código QR previamente en la Panamericana Sur)
        boolean yaAnulado = false;
        boolean fueValidadoPorInspector = true;

        // 2. ACT (Actuar: Cobrador intenta procesar la anulación por lista)
        String resultado = processor.evaluarAnulacionBoleto(yaAnulado, fueValidadoPorInspector);

        // 3. ASSERT (Verificar: Debe saltar el escudo con la glosa reglamentaria exacta)
        assertEquals(TicketCancellationProcessor.MSG_ERROR_INSPECTOR, resultado);
    }
}