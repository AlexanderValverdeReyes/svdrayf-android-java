package com.alexander.pasajes.ui.printer;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class PrinterPaperProcessorTest {

    private PrinterPaperProcessor processor;

    @Before
    public void setUp() {
        // Inicialización de la máquina de estados de telemetría de papel
        processor = new PrinterPaperProcessor();
    }

    // CP106: ESCENARIO EXITOSO — VERIFICACIÓN CORRECTA DE PAPEL (Happy Path)
    @Test
    public void cp106_debePermitirImpresionSinAlertasSiElSensorEsCompatibleYHayPapel() {
        // 1. ARRANGE (Preparar: Rollo térmico colocado y sensor plenamente compatible)
        boolean sensorCompatible = true;
        boolean tienePapelSuficiente = true;

        // 2. ACT (Actuar: Solicitar la validación del estado del rodillo antes de imprimir)
        String resultado = processor.evaluarEstadoPapel(sensorCompatible, tienePapelSuficiente);

        // 3. ASSERT (Verificar: Debe responder con código de éxito para impresión limpia)
        assertEquals(PrinterPaperProcessor.STATUS_PAPER_OK, resultado);
    }

    // CP107: SENSOR DE LA TICKETERA INOPERATIVO O NO COMPATIBLE (Sad Path)
    @Test
    public void cp107_debeIgnorarSensorPermitirImpresionYMostrarNotaDeAdvertencia() {
        // 1. ARRANGE (Preparar: Ticketera china sin soporte de retorno de bytes de estado de papel)
        boolean sensorCompatible = false;
        boolean tienePapelSuficiente = true; // No se puede leer con certeza por hardware

        // 2. ACT (Actuar: Intentar enviar una orden de impresión de pasaje)
        String resultado = processor.evaluarEstadoPapel(sensorCompatible, tienePapelSuficiente);

        // 3. ASSERT (Verificar: El sistema debe permitir la salida física inyectando la glosa mandatoria)
        assertEquals(PrinterPaperProcessor.MSG_WARN_SENSOR_INCOMPATIBLE, resultado);
    }
}