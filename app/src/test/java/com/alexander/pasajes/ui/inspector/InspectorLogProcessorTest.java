package com.alexander.pasajes.ui.inspector; //

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class InspectorLogProcessorTest {

    private InspectorLogProcessor processor;

    @Before
    public void setUp() {
        // Inicialización de la máquina de estados de validación de bitácoras
        processor = new InspectorLogProcessor();
    }

    // CP115: ESCENARIO EXITOSO — REGISTRO CORRECTO DE BITÁCORA (Happy Path)
    @Test
    public void cp115_debePermitirAlmacenamientoSiLosCamposContablesSonConformes() {
        // 1. ARRANGE (Preparar: Fiscalizador ingresa conteo válido de 45 pasajeros en viaje regular)
        int pasajerosFisicos = 45;
        String observaciones = "Inspección completada con normalidad.";
        boolean isBoletoValidoYActivo = true;

        // 2. ACT (Actuar: Solicitar el dictamen de validación de la bitácora)
        String resultado = processor.evaluarBitacoraControl(pasajerosFisicos, observaciones, isBoletoValidoYActivo);

        // 3. ASSERT (Verificar: El sistema debe aprobar la bitácora para enviarla a Neon DB)
        assertEquals(InspectorLogProcessor.STATUS_LOG_CONFORME, resultado);
    }

    // CP116: ERROR — FORMULARIO CON CANTIDAD VACÍA O EN CERO (Sad Path)
    @Test
    public void cp116_debeBloquearEnvioYNotificarSiLaCantidadDePasajerosEsInvalida() {
        // 1. ARRANGE (Preparar: Campo de conteo de boletos ingresado en cero)
        int pasajerosFisicos = 0;
        String observaciones = "Evasión potencial.";
        boolean isBoletoValidoYActivo = false;

        // 2. ACT (Actuar: Presionar el botón digital de guardar reporte)
        String resultado = processor.evaluarBitacoraControl(pasajerosFisicos, observaciones, isBoletoValidoYActivo);

        // 3. ASSERT (Verificar: Debe saltar la alerta restrictiva obligando a ingresar montos válidos)
        assertEquals(InspectorLogProcessor.MSG_ERROR_ZERO_OR_EMPTY, resultado);
    }
}