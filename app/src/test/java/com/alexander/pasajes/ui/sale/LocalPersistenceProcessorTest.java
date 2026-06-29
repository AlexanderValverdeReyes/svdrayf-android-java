package com.alexander.pasajes.ui.sale;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class LocalPersistenceProcessorTest {

    private LocalPersistenceProcessor processor;

    @Before
    public void setUp() {
        // Inicialización del motor analítico de almacenamiento relacional
        processor = new LocalPersistenceProcessor();
    }

    // CP100: ESCENARIO EXITOSO — GUARDADO CORRECTO DE BOLETO (Happy Path)
    @Test
    public void cp100_debeGuardarBoletoCorrectamenteSiElAlmacenamientoEsConforme() {
        // 1. ARRANGE (Preparar: Dispositivo con almacenamiento libre y código único)
        boolean almacenamientoLleno = false;
        boolean identificadorDuplicado = false;

        // 2. ACT (Actuar: Solicitar el registro del boleto en Room local)
        String resultado = processor.evaluarPersistenciaLocal(almacenamientoLleno, identificadorDuplicado);

        // 3. ASSERT (Verificar: El sistema debe confirmar la persistencia y preparar sincronización)
        assertEquals(LocalPersistenceProcessor.STATUS_PERSIST_OK, resultado);
    }

    // CP101: ERROR — ALMACENAMIENTO INTERNO DEL CELULAR LLENO (Sad Path)
    @Test
    public void cp101_debeCancelarOperacionYMostrarAlertaSiNoHayEspacioInterno() {
        // 1. ARRANGE (Preparar: Sistema de archivos bloqueado por falta de espacio en disco)
        boolean almacenamientoLleno = true;
        boolean identificadorDuplicado = false;

        // 2. ACT (Actuar: Intentar confirmar el grabado del pasaje en el pasillo del bus)
        String resultado = processor.evaluarPersistenciaLocal(almacenamientoLleno, identificadorDuplicado);

        // 3. ASSERT (Verificar: Debe saltar la advertencia mandatoria y frenar la persistencia)
        assertEquals(LocalPersistenceProcessor.MSG_ERROR_STORAGE_FULL, resultado);
    }

    // CP102: EXCEPCIÓN — IDENTIFICADOR DE REGISTRO DUPLICADO (Sad Path)
    @Test
    public void cp102_debeDetectarCodigoRepetidoYSustituirConDatosLimpios() {
        // 1. ARRANGE (Preparar: Intento accidental de colisión de clave primaria local)
        boolean almacenamientoLleno = false;
        boolean identificadorDuplicado = true;

        // 2. ACT (Actuar: Procesar automáticamente la reparación asíncrona de Room)
        String resultado = processor.evaluarPersistenciaLocal(almacenamientoLleno, identificadorDuplicado);

        // 3. ASSERT (Verificar: El núcleo debe autocorregir la redundancia salvando la venta)
        assertEquals(LocalPersistenceProcessor.STATUS_DUPLICATE_FIXED, resultado);
    }
}