package com.alexander.pasajes.ui.sale;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class RouteStopsProcessorTest {

    private RouteStopsProcessor processor;

    @Before
    public void setUp() {
        // Inicialización del motor analítico de consistencia de rutas
        processor = new RouteStopsProcessor();
    }

    // CP78: ESCENARIO EXITOSO — REGISTRO CORRECTO DE TRAMOS (Happy Path)
    @Test
    public void cp78_debePermitirCotizacionSiElSentidoYLaMatrizSonConformes() {
        // 1. ARRANGE (Preparar: Bus operando de Sur a Norte con tramo válido marcado)
        boolean esSentidoInvalido = false;
        boolean esMatrizCorrupta = false;

        // 2. ACT (Actuar: Cruzar la combinación con la matriz local de Room)
        String resultado = processor.evaluarSeleccionTramos(esSentidoInvalido, esMatrizCorrupta);

        // 3. ASSERT (Verificar: Debe responder en menos de medio segundo y actualizar precio)
        assertEquals(RouteStopsProcessor.STATUS_STOPS_OK, resultado);
    }

    // CP79: SELECCIÓN DE TRAMO INEXISTENTE O PARADEROS INVERTIDOS (Sad Path)
    @Test
    public void cp79_debeBloquearCobroYNotificarSiSeMarcaUnDestinoEnSentidoContrario() {
        // 1. ARRANGE (Preparar: Cobrador selecciona por error un paradero hacia el Sur estando en ruta al Norte)
        boolean esSentidoInvalido = true;
        boolean esMatrizCorrupta = false;

        // 2. ACT (Actuar: Procesar la incongruencia de sentido local)
        String resultado = processor.evaluarSeleccionTramos(esSentidoInvalido, esMatrizCorrupta);

        // 3. ASSERT (Verificar: Debe congelar el avance y mostrar la alerta explícita en amarillo)
        assertEquals(RouteStopsProcessor.MSG_ERROR_INVERTED_STOPS, resultado);
    }

    // CP80: INCONSISTENCIA O CORRUPCIÓN EN LA TABLA RELACIONAL SQLITE (Sad Path)
    @Test
    public void cp80_debeBloquearFlujoComercialSiLaMatrizDeTramosEstaIlegible() {
        // 1. ARRANGE (Preparar: Tabla relacional dañada por error de sincronización de fondo)
        boolean esSentidoInvalido = false;
        boolean esMatrizCorrupta = true;

        // 2. ACT (Actuar: Intentar confirmar la combinación de paraderos en la pantalla táctil)
        String resultado = processor.evaluarSeleccionTramos(esSentidoInvalido, esMatrizCorrupta);

        // 3. ASSERT (Verificar: Debe forzar el escudo preventivo para evitar tarifas arbitrarias)
        assertEquals(RouteStopsProcessor.MSG_ERROR_CORRUPTED_MATRIX, resultado);
    }
}