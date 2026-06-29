package com.alexander.pasajes.ui.history;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class DataConflictProcessorTest {

    private DataConflictProcessor processor;

    @Before
    public void setUp() {
        // Inicialización de la máquina de estados de conciliación de datos
        processor = new DataConflictProcessor();
    }

    // CP133: ESCENARIO EXITOSO — CONCILIACIÓN DE REGISTROS POR FECHA (Happy Path)
    @Test
    public void cp133_debeConservarElPasajeMasRecienteCuandoSeDetectaMismoCodigoConDatosDiferentes() {
        // 1. ARRANGE (Preparar: Colisión de código, datos mutados en ruta y hora local posterior)
        boolean mismoCodigo = true;
        boolean datosDiferentes = true;
        long fechaLocal = 1719641600000L;    // Timestamp más reciente
        long fechaServidor = 1719638000000L; // Timestamp antiguo
        boolean esIdentico = false;

        // 2. ACT (Actuar: Solicitar al procesador dirimir la colisión cronológica)
        String resultado = processor.evaluarResolucionConflicto(mismoCodigo, datosDiferentes, fechaLocal, fechaServidor, esIdentico);

        // 3. ASSERT (Verificar: El sistema debe dar luz verde para preservar el registro nuevo)
        assertEquals(DataConflictProcessor.STATUS_CONSERVAR_RECIENTE, resultado);
    }

    // CP134: ERROR — INTENTO DE SUBIDA CON DATOS EXACTAMENTE IDÉNTICOS (Sad Path)
    @Test
    public void cp134_debeOmitirYIgnorarElRegistroSiLosDatosYaExistenExactamenteIgualesEnElServidor() {
        // 1. ARRANGE (Preparar: Fila idéntica ya persistida con anterioridad en las tablas cloud)
        boolean mismoCodigo = true;
        boolean datosDiferentes = false;
        long fechaLocal = 1719638000000L;
        long fechaServidor = 1719638000000L;
        boolean esIdentico = true;

        // 2. ACT (Actuar: Evaluar el paquete redundante enviado en la ráfaga)
        String resultado = processor.evaluarResolucionConflicto(mismoCodigo, datosDiferentes, fechaLocal, fechaServidor, esIdentico);

        // 3. ASSERT (Verificar: Debe forzar la instrucción de ignorar la entrada mitigando duplicidades)
        assertEquals(DataConflictProcessor.STATUS_IGNORAR_DUPLICADO, resultado);
    }
}