package com.alexander.pasajes.ui.inspector;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class QrAuthenticityProcessorTest {

    private QrAuthenticityProcessor processor;

    @Before
    public void setUp() {
        // Inicialización del motor analítico de autenticidad criptográfica
        processor = new QrAuthenticityProcessor();
    }

    // CP119: ESCENARIO EXITOSO — CONFIRMACIÓN DE BOLETO ORIGINAL (Happy Path)
    @Test
    public void cp119_debeConfirmarBoletoOriginalSiExisteYEsPrimeraVezQueSeEscanea() {
        // 1. ARRANGE (Preparar: Código QR pertenece al sistema y no registra lecturas previas)
        boolean existeEnSistema = true;
        boolean yaEscaneadoEnBus = false;

        // 2. ACT (Actuar: Analizar la autenticidad en el pasillo del bus)
        String resultado = processor.evaluarAutenticidadQr(existeEnSistema, yaEscaneadoEnBus);

        // 3. ASSERT (Verificar: Debe aprobar con estado conforme y pintar pantalla en verde)
        assertEquals(QrAuthenticityProcessor.STATUS_AUTHENTIC_OK, resultado);
    }

    // CP120: ERROR — CÓDIGO QR NO RECONOCIDO EN EL SISTEMA (Sad Path)
    @Test
    public void cp120_debeBloquearAprobacionYMostrarErrorSiElQrNoExisteEnBaseDeDatos() {
        // 1. ARRANGE (Preparar: Inspector escanea código ajeno o de otra aplicación)
        boolean existeEnSistema = false;
        boolean yaEscaneadoEnBus = false;

        // 2. ACT (Actuar: Intentar validar la firma en la central)
        String resultado = processor.evaluarAutenticidadQr(existeEnSistema, yaEscaneadoEnBus);

        // 3. ASSERT (Verificar: Debe frenar la validación y forzar la glosa exacta exigida)
        assertEquals(QrAuthenticityProcessor.MSG_ERROR_NOT_RECONOCIDO, resultado);
    }

    // CP121: EXCEPCIÓN — BOLETO YA VERIFICADO / QR DUPLICADO (Sad Path)
    @Test
    public void cp121_debeDetenerAprobacionYMostrarMensajeSiSeVuelveAEscanearElMismoQr() {
        // 1. ARRANGE (Preparar: Boleto legítimo pero escaneado por segunda vez consecutiva)
        boolean existeEnSistema = true;
        boolean yaEscaneadoEnBus = true;

        // 2. ACT (Actuar: Procesar la colisión de firma en el terminal móvil)
        String resultado = processor.evaluarAutenticidadQr(existeEnSistema, yaEscaneadoEnBus);

        // 3. ASSERT (Verificar: Debe emitir la advertencia de pasaje ya verificado para evitar fraude)
        assertEquals(QrAuthenticityProcessor.MSG_ERROR_DUPLICADO, resultado);
    }
}