package com.alexander.pasajes.ui.inspector;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class QrScannerProcessorTest {

    private QrScannerProcessor processor;

    @Before
    public void setUp() {
        // Inicialización de la máquina de estados del escáner de la cámara
        processor = new QrScannerProcessor();
    }

    // CP110: ESCENARIO EXITOSO — LECTURA CORRECTA DE CÓDIGO QR (Happy Path)
    @Test
    public void cp110_debeRetornarExitoSiSeEnfocaUnBoletoEnBuenEstadoYHayPermiso() {
        // 1. ARRANGE (Preparar: Permisos concedidos y lectura instantánea de la matriz)
        boolean tienePermisoCamara = true;
        boolean escaneoCompletado = true;
        int tiempoEsperaSegundos = 0;

        // 2. ACT (Actuar: Solicitar el procesamiento criptográfico del QR)
        String resultado = processor.evaluarEscaneoQr(tienePermisoCamara, escaneoCompletado, tiempoEsperaSegundos);

        // 3. ASSERT (Verificar: Debe responder con código conforme para decodificar en Neon DB)
        assertEquals(QrScannerProcessor.STATUS_SCAN_SUCCESS, resultado);
    }

    // CP111: ERROR — CÓDIGO QR DETERIORADO, ROTO O ILEGIBLE (Sad Path)
    @Test
    public void cp107_debeGatillarEntradaManualSiElEscanerSuperaLosTresSegundos() {
        // 1. ARRANGE (Preparar: Código borroso por desgaste térmico, el visor no decodifica)
        boolean tienePermisoCamara = true;
        boolean escaneoCompletado = false;
        int tiempoEsperaSegundos = 3;

        // 2. ACT (Actuar: Transcurre el tiempo límite de enfoque en el pasillo)
        String resultado = processor.evaluarEscaneoQr(tienePermisoCamara, escaneoCompletado, tiempoEsperaSegundos);

        // 3. ASSERT (Verificar: Debe exigir de forma estricta la glosa de digitación manual)
        assertEquals(QrScannerProcessor.MSG_ERROR_ILEGIBLE, resultado);
    }

    // CP112: EXCEPCIÓN — PERMISOS DE CÁMARA DESACTIVADOS (Sad Path)
    @Test
    public void cp112_debeBloquearAccionYNotificarSiFaltaLaAutorizacionDelSistemaOperativo() {
        // 1. ARRANGE (Preparar: Inspector bloqueó u omitió otorgar permisos de cámara en los ajustes)
        boolean tienePermisoCamara = false;
        boolean escaneoCompletado = false;
        int tiempoEsperaSegundos = 0;

        // 2. ACT (Actuar: Presionar el botón digital "Escanear QR")
        String resultado = processor.evaluarEscaneoQr(tienePermisoCamara, escaneoCompletado, tiempoEsperaSegundos);

        // 3. ASSERT (Verificar: Debe congelar el hardware lanzando el mensaje denegado exacto)
        assertEquals(QrScannerProcessor.MSG_ERROR_PERMISOS, resultado);
    }
}