package com.alexander.pasajes.ui.inspector;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class InspectorHistoryProcessorTest {

    private InspectorHistoryProcessor processor;

    @Before
    public void setUp() {
        // Inicialización de la máquina de estados de lectura de bitácoras pasadas
        processor = new InspectorHistoryProcessor();
    }

    // CP117: ESCENARIO EXITOSO — VISUALIZACIÓN DE REVISIONES (Happy Path)
    @Test
    public void cp117_debeAprobarDespliegueSiExistenRegistrosDeInspeccionesPasadas() {
        // 1. ARRANGE (Preparar: Neon DB devuelve una lista con 5 buses controlados en el día)
        int cantidadRegistros = 5;

        // 2. ACT (Actuar: Solicitar al procesador evaluar el estado del historial)
        String resultado = processor.evaluarHistorialFiscalizaciones(cantidadRegistros);

        // 3. ASSERT (Verificar: El sistema debe dar luz verde para poblar el RecyclerView)
        assertEquals(InspectorHistoryProcessor.STATUS_READ_OK, resultado);
    }

    // CP118: ERROR — AUSENCIA DE REGISTROS / HISTORIAL VACÍO (Sad Path)
    @Test
    public void cp118_debeLimpiarPantallaYMostrarMensajeInformativoSiElHistorialEstaVacio() {
        // 1. ARRANGE (Preparar: Inspector inicia su jornada y no registra auditorías previas)
        int cantidadRegistros = 0;

        // 2. ACT (Actuar: Presionar la opción "Ver Historial" en el menú principal)
        String resultado = processor.evaluarHistorialFiscalizaciones(cantidadRegistros);

        // 3. ASSERT (Verificar: Debe inyectar en la UI la glosa informativa exacta del Excel)
        assertEquals(InspectorHistoryProcessor.MSG_EMPTY_HISTORY, resultado);
    }
}