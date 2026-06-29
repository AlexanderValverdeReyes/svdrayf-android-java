package com.alexander.pasajes.ui.inspector; // 🟢 Ubicación simétrica en tu suite de pruebas locales

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class ValidationFeedbackProcessorTest {

    private ValidationFeedbackProcessor processor;

    @Before
    public void setUp() {
        // Inicialización del motor analítico cromático de fiscalización
        processor = new ValidationFeedbackProcessor();
    }

    // =========================================================================
    // CP113: ESCENARIO EXITOSO — MUESTRA DE AUDITORÍA CONFORME (Happy Path)
    // =========================================================================
    @Test
    public void cp113_debeRetornarVerdeSiElBoletoEsValidoYElTurnoEstaAbierto() {
        // 1. ARRANGE (Preparar: Registro emparejado limpiamente en Neon DB)
        boolean encontradoEnCentral = true;
        String estadoBoleto = "VALIDO";
        String estadoTurno = "ABIERTO";

        // 2. ACT (Actuar: Solicitar el dictamen de feedback visual para el fragmento)
        String resultado = processor.evaluarFeedbackVisual(encontradoEnCentral, estadoBoleto, estadoTurno);

        // 3. ASSERT (Verificar: La interfaz debe cambiar a pantalla completa a color verde)
        assertEquals(ValidationFeedbackProcessor.COLOR_VERDE_CONFORME, resultado);
    }

    // =========================================================================
    // CP114: ERROR — CÓDIGO QR NO RECONOCIDO EN CENTRAL WEB (Sad Path)
    // =========================================================================
    @Test
    public void cp114_debeRetornarErrorNoReconocidoSiElQrNoExisteEnElServidor() {
        // 1. ARRANGE (Preparar: Token QR de otra empresa o alterado maliciosamente)
        boolean encontradoEnCentral = false;
        String estadoBoleto = "INEXISTENTE";
        String estadoTurno = "INEXISTENTE";

        // 2. ACT (Actuar: Cruzar el hash contra la base de datos cloud)
        String resultado = processor.evaluarFeedbackVisual(encontradoEnCentral, estadoBoleto, estadoTurno);

        // 3. ASSERT (Verificar: Debe forzar la pantalla roja gatillando la glosa restrictiva)
        assertEquals(ValidationFeedbackProcessor.MSG_ERROR_NO_RECONOCIDO, resultado);
    }
}