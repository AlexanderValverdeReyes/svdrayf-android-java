package com.alexander.pasajes.ui.sale;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class AuditAlertProcessorTest {

    private AuditAlertProcessor processor;

    @Before
    public void setUp() {
        // Inicialización del motor perimetral de auditoría interna
        processor = new AuditAlertProcessor();
    }

    // CP84: ESCENARIO EXITOSO — REGISTRO CORRECTO DE MÉTODO DE PAGO (Happy Path)
    @Test
    public void cp84_debeRegistrarBoletoSinAlertasSiLaSeleccionEsDirecta() {
        // 1. ARRANGE (Preparar: Cobrador selecciona el método solicitado de forma limpia)
        boolean abrioQrPreviamente = false;
        String metodoPagoFinal = "EFECTIVO";
        int cantidadCambios = 1;

        // 2. ACT (Actuar: Analizar el comportamiento de la pulsación)
        String resultado = processor.evaluarAlertaAuditoria(abrioQrPreviamente, metodoPagoFinal, cantidadCambios);

        // 3. ASSERT (Verificar: El sistema no debe generar ninguna marca de auditoría)
        assertEquals(AuditAlertProcessor.STATUS_CONFORME, resultado);
    }

    // CP85: CAMBIO MÚLTIPLE O INTERRUPCIÓN DE PROYECCIÓN DIGITAL (Sad Path / Anomalía)
    @Test
    public void cp85_debeDispararAlertaOcultaSiExisteAlternanciaSospechosa() {
        // 1. ARRANGE (Preparar: El cobrador proyectó el QR pero cerró el canal para exigir efectivo)
        boolean abrioQrPreviamente = true;
        String metodoPagoFinal = "EFECTIVO";
        int cantidadCambios = 3; // Cobrador osciló repetidamente los controles táctiles

        // 2. ACT (Actuar: Evaluar riesgo de fraude o desvío de flujo de recaudación)
        String resultado = processor.evaluarAlertaAuditoria(abrioQrPreviamente, metodoPagoFinal, cantidadCambios);

        // 3. ASSERT (Verificar: Debe encender de forma permanente las banderas de control para la base web)
        assertEquals(AuditAlertProcessor.STATUS_ALERTA_FRAUDE, resultado);
    }
}