package com.alexander.pasajes.ui.cobrador;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class RouteSelectionProcessorTest {

    private RouteSelectionProcessor processor;

    @Before
    public void setUp() {
        // Inicialización global del procesador analítico de rutas
        processor = new RouteSelectionProcessor();
    }

    // CP69: ESCENARIO EXITOSO — MODALIDAD DE VIAJE CONFORME (Happy Path)
    @Test
    public void cp69_debeAprobarModalidadSiElTarifarioEstaCompletoEnElTerminal() {
        // 1. ARRANGE (Preparar: Cobrador inicializando en Mala con catálogo íntegro)
        boolean tieneTransaccionesPrevias = false;
        boolean tarifarioIncompleto = false;
        String modalidadSeleccionada = "PARADEROS";

        // 2. ACT (Actuar: Confirmar la modalidad seleccionada en el spinner)
        String resultado = processor.evaluarConfiguracionRuta(tieneTransaccionesPrevias, tarifarioIncompleto, modalidadSeleccionada);

        // 3. ASSERT (Verificar: Debe inyectar los tramos estáticos y dar pase conforme)
        assertEquals(RouteSelectionProcessor.STATUS_ROUTE_OK, resultado);
    }

    // CP70: INTENTO DE ALTERACIÓN DE MODALIDAD A MITAD DE RUTA (Sad Path)
    @Test
    public void cp70_debeBloquearConmutacionSiYaExistenBoletosGrabadosEnSQLite() {
        // 1. ARRANGE (Preparar: Bus en movimiento que ya registra boletaje activo)
        boolean tieneTransaccionesPrevias = true;
        boolean tarifarioIncompleto = false;
        String modalidadSeleccionada = "PARADEROS";

        // 2. ACT (Actuar: El cobrador intenta regresar de forma arbitraria a cambiar la ruta)
        String resultado = processor.evaluarConfiguracionRuta(tieneTransaccionesPrevias, tarifarioIncompleto, modalidadSeleccionada);

        // 3. ASSERT (Verificar: El sistema debe congelar la acción con el mensaje de seguridad contable)
        assertEquals(RouteSelectionProcessor.MSG_ERROR_MIDWAY_CHANGE, resultado);
    }

    // CP71: CORRUPCIÓN DE DATOS O AUSENCIA DE TRAMOS EN SQLITE (Sad Path)
    @Test
    public void cp71_debeDetenerAperturaSiLaEstructuraDeTarifasSufrioSincronizacionTrunca() {
        // 1. ARRANGE (Preparar: Tabla interna de tarifas incompleta para tramos intermedios)
        boolean tieneTransaccionesPrevias = false;
        boolean tarifarioIncompleto = true;
        String modalidadSeleccionada = "PARADEROS";

        // 2. ACT (Actuar: Intentar abrir la ruta bajo la modalidad por paraderos)
        String resultado = processor.evaluarConfiguracionRuta(tieneTransaccionesPrevias, tarifarioIncompleto, modalidadSeleccionada);

        // 3. ASSERT (Verificar: Debe forzar el bloqueo exigiendo reaprovisionamiento en central)
        assertEquals(RouteSelectionProcessor.MSG_ERROR_CORRUPTED_FARES, resultado);
    }
}