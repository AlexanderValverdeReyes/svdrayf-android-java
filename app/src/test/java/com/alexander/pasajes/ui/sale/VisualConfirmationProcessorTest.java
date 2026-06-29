package com.alexander.pasajes.ui.sale;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class VisualConfirmationProcessorTest {

    private VisualConfirmationProcessor processor;

    @Before
    public void setUp() {
        // Inicialización de la máquina de estados de confirmación visual
        processor = new VisualConfirmationProcessor();
    }

    // CP108: ESCENARIO EXITOSO — DESPLIEGUE CORRECTO DE CONFIRMACIÓN (Happy Path)
    @Test
    public void cp108_debeRetornarConfirmadoSiLaInfeccionEnRoomLocalFueExitosa() {
        // 1. ARRANGE (Preparar: Simulación de guardado exitoso del boleto en SQLite local)
        boolean guardadoExitoso = true;

        // 2. ACT (Actuar: Solicitar el dictamen visual al finalizar el almacenamiento de la venta)
        String resultado = processor.evaluarConfirmacionVisual(guardadoExitoso);

        // 3. ASSERT (Verificar: El sistema debe activar el mensaje de confirmación en la UI)
        assertEquals(VisualConfirmationProcessor.STATUS_CONFIRMED, resultado);
    }
}