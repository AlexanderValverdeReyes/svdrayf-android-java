package com.alexander.pasajes.ui.history;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class ShiftCashCuadreProcessorTest {

    private ShiftCashCuadreProcessor processor;

    @Before
    public void setUp() {
        // Inicialización del motor analítico de balances de caja
        processor = new ShiftCashCuadreProcessor();
    }

    // CP91: ESCENARIO EXITOSO — VISUALIZACIÓN CORRECTA DE CAJA (Happy Path)
    @Test
    public void cp91_debeAprobarArqueoSiExistenVentasYLosRegistrosSonConsistentes() {
        // 1. ARRANGE (Preparar: Turno con 15 pasajes vendidos y sin corrupciones de hardware)
        int totalBoletos = 15;
        boolean registrosCorruptosDetectados = false;

        // 2. ACT (Actuar: Solicitar la evaluación del estado del cuadre operativo)
        String resultado = processor.evaluarEstadoCuadre(totalBoletos, registrosCorruptosDetectados);

        // 3. ASSERT (Verificar: El sistema debe validar el balance de forma conforme)
        assertEquals(ShiftCashCuadreProcessor.STATUS_CUADRE_OK, resultado);
    }

    // CP92: INEXISTENCIA DE TRANSACCIONES EN EL TURNO ACTUAL (Happy Path / Empty)
    @Test
    public void cp92_debeColocarContadoresEnCeroYMostrarMensajeDeTurnoInicializado() {
        // 1. ARRANGE (Preparar: Jornada recién abierta en paradero, total de boletos es 0)
        int totalBoletos = 0;
        boolean registrosCorruptosDetectados = false;

        // 2. ACT (Actuar: Procesar la visualización del arqueo de caja inicial)
        String resultado = processor.evaluarEstadoCuadre(totalBoletos, registrosCorruptosDetectados);

        // 3. ASSERT (Verificar: Debe forzar la inyección de la glosa de turno limpio sin ventas)
        assertEquals(ShiftCashCuadreProcessor.MSG_EMPTY_SHIFT, resultado);
    }

    // CP93: DESALINEACIÓN O CORRUPCIÓN DE REGISTROS LOCALES (Sad Path / Contingencia)
    @Test
    public void cp93_debeMostrarBalanceRealYAnexarAvisoAmarilloDeRecomendacionCloud() {
        // 1. ARRANGE (Preparar: Teléfono sufrió un apagado imprevisto por falta de batería severa)
        int totalBoletos = 8;
        boolean registrosCorruptosDetectados = true;

        // 2. ACT (Actuar: Recuperar datos limpios y procesar el balance de emergencia)
        String resultado = processor.evaluarEstadoCuadre(totalBoletos, registrosCorruptosDetectados);

        // 3. ASSERT (Verificar: El núcleo debe alertar la desalineación recomendando sincronizar)
        assertEquals(ShiftCashCuadreProcessor.MSG_WARN_CORRUPTED, resultado);
    }
}