package com.alexander.pasajes.ui.sale;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class TicketPrintProcessorTest {

    private TicketPrintProcessor processor;

    @Before
    public void setUp() {
        // Inicialización global del procesador analítico de impresión
        processor = new TicketPrintProcessor();
    }

    // CP86: ESCENARIO EXITOSO — IMPRESIÓN CORRECTA DE BOLETO (Happy Path)
    @Test
    public void cp86_debePermitirImpresionEstandarSiElDispositivoEstaSanoYElCobroEsExitoso() {
        // 1. ARRANGE (Preparar: Pasaje cobrado con éxito y ticketera operativa)
        boolean boletoCobrado = true;
        boolean errorMecanicoPapel = false;
        boolean errorConexionBluetooth = false;
        boolean intentoReimpresion = false;

        // 2. ACT (Actuar: Solicitar el estado del payload inicial)
        String resultado = processor.evaluarSolicitudImpresion(boletoCobrado, errorMecanicoPapel, errorConexionBluetooth, intentoReimpresion);

        // 3. ASSERT (Verificar: Debe aprobar el envío regular de datos a la ticketera)
        assertEquals(TicketPrintProcessor.STATUS_PRINT_OK, resultado);
    }

    // CP87: CONTINGENCIA MECÁNICA — ATASCO O FIN DE ROLLO DE PAPEL (Sad Path)
    @Test
    public void cp87_debeHabilitarReimpresionEInyectarGlosaDeAuditoriaSiHuboAtasco() {
        // 1. ARRANGE (Preparar: Pasaje cobrado pero el papel se traba en plena impresión)
        boolean boletoCobrado = true;
        boolean errorMecanicoPapel = true;
        boolean errorConexionBluetooth = false;
        boolean intentoReimpresion = true;

        String payloadOriginal = "BOLETO_MALA_LIMA_1025";

        // 2. ACT (Actuar: Evaluar la solicitud y formatear la trama binaria)
        String dictamen = processor.evaluarSolicitudImpresion(boletoCobrado, errorMecanicoPapel, errorConexionBluetooth, intentoReimpresion);
        String payloadModificado = processor.formatearPayloadSeguro(payloadOriginal, true);

        // 3. ASSERT (Verificar: Debe destrabar el hardware anexando la advertencia sin duplicar venta)
        assertEquals(TicketPrintProcessor.STATUS_CAN_REPRINT_JAM, dictamen);
        assertTrue(payloadModificado.contains(TicketPrintProcessor.GLOSA_ATASCO));
    }

    // CP88: FALLA DE ENLACE — DESCONEXIÓN DE SEÑAL BLUETOOTH (Sad Path)
    @Test
    public void cp88_debeHabilitarRevinculacionYReenvioSiSeDetectaCorteDeRadiofrecuencia() {
        // 1. ARRANGE (Preparar: Pérdida de paquetes inalámbricos en el pasillo del bus)
        boolean boletoCobrado = true;
        boolean errorMecanicoPapel = false;
        boolean errorConexionBluetooth = true;
        boolean intentoReimpresion = true;

        // 2. ACT (Actuar: Procesar la solicitud del control "Re-vincular e Imprimir")
        String resultado = processor.evaluarSolicitudImpresion(boletoCobrado, errorMecanicoPapel, errorConexionBluetooth, intentoReimpresion);

        // 3. ASSERT (Verificar: Debe dar luz verde al reenvío físico tras restaurar el canal)
        assertEquals(TicketPrintProcessor.STATUS_CAN_REPRINT_DISCONNECT, resultado);
    }
}