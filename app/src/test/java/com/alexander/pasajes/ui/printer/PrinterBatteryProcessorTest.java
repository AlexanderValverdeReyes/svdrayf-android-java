package com.alexander.pasajes.ui.printer;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class PrinterBatteryProcessorTest {

    private PrinterBatteryProcessor batteryProcessor;

    @Before
    public void setUp() {
        batteryProcessor = new PrinterBatteryProcessor();
    }

    // CP62: ESCENARIO EXITOSO — TELEMETRÍA DE CARGA CONFORME (Happy Path)
    @Test
    public void cp62_debeRetornarEstadoConformeSiLaBateriaEsSuficienteYHayEnlace() {
        // 1. ARRANGE: Simula una trama binaria válida que devuelve 85% de energía
        boolean enlaceBluetoothActivo = true;
        byte[] tramaBateriaOchentaYCinco = new byte[]{ 85 };

        // 2. ACT: Dispara el extractor y el evaluador analítico de producción
        String resultado = batteryProcessor.evaluarMonitoreoBateria(enlaceBluetoothActivo, tramaBateriaOchentaYCinco);

        // 3. ASSERT: El núcleo debe aprobar la telemetría para habilitar la interfaz
        assertEquals(PrinterBatteryProcessor.STATUS_TELEMETRY_OK, resultado);
    }

    // CP63: DESCONEXIÓN O PÉRDIDA ABRUPTA DEL ENLACE BLUETOOTH (Sad Path)
    @Test
    public void cp63_debeBloquearEmisionYNotificarCaidaDeCanalLogico() {
        // 1. ARRANGE: Pérdida total de señal inalámbrica en la carretera interprovincial
        boolean enlaceBluetoothActivo = false;
        byte[] tramaVaciaPorDesconexion = null;

        // 2. ACT: El hilo secundario intenta procesar la telemetría periódica
        String resultado = batteryProcessor.evaluarMonitoreoBateria(enlaceBluetoothActivo, tramaVaciaPorDesconexion);

        // 3. ASSERT: Debe forzar la detención por el aviso crítico de hardware
        assertEquals(PrinterBatteryProcessor.MSG_ERROR_DISCONNECTED, resultado);
    }

    // CP64: DESBORDE POR BATERÍA EN NIVEL CRÍTICO SEVERO (Sad Path)
    @Test
    public void cp64_debeBloquearTramasDeImpresionSiLaCargaEsMenorAlDiezPorCiento() {
        // 1. ARRANGE: Enviamos un porcentaje directo (8%) y una trama de hardware con flag (0x8C)
        boolean enlaceBluetoothActivo = true;
        byte[][] tramasCriticas = new byte[][]{
                new byte[]{ 8 },              // 8% Directo (Caso 1)
                new byte[]{ (byte) 0x8C }     // Registro ESC/POS mapeado a 5% Crítico (Caso 2)
        };

        // 2. ACT & 3. ASSERT
        for (byte[] tramaCritica : tramasCriticas) {
            String resultado = batteryProcessor.evaluarMonitoreoBateria(enlaceBluetoothActivo, tramaCritica);
            assertEquals(PrinterBatteryProcessor.MSG_ERROR_CRITICAL_BATTERY, resultado);
        }
    }
}