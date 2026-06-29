package com.alexander.pasajes.ui.sale;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class DoubleShiftProcessorTest {

    private DoubleShiftProcessor processor;

    @Before
    public void setUp() {
        // Inicialización de la máquina de estados de concurrencia
        processor = new DoubleShiftProcessor();
    }

    // CP103: ESCENARIO EXITOSO — APERTURA CORRECTA DE JORNADA (Happy Path)
    @Test
    public void cp103_debePermitirAccesoSiNoExistenTurnosActivosVinculados() {
        // 1. ARRANGE (Preparar: Servidor limpio y hardware con batería conforme)
        boolean tieneTurnoActivoServidor = false;
        boolean sufrioApagadoImprevisto = false;

        // 2. ACT (Actuar: Solicitar la verificación perimetral de la jornada)
        String resultado = processor.evaluarDobleTurno(tieneTurnoActivoServidor, sufrioApagadoImprevisto);

        // 3. ASSERT (Verificar: Debe otorgar el pase conforme para proceder con el cobro)
        assertEquals(DoubleShiftProcessor.STATUS_SHIFT_OK, resultado);
    }

    // CP104: ERROR DE ESCRITURA POR APAGADO DE HARDWARE (Sad Path)
    @Test
    public void cp104_debeForzarRetomoDeViajeAnteriorSiElCelularSeApagoSinBateria() {
        // 1. ARRANGE (Preparar: Aplicación detecta cierre trunco por falta de energía)
        boolean tieneTurnoActivoServidor = false;
        boolean sufrioApagadoImprevisto = true;

        // 2. ACT (Actuar: Intentar inicializar un nuevo viaje en la terminal)
        String resultado = processor.evaluarDobleTurno(tieneTurnoActivoServidor, sufrioApagadoImprevisto);

        // 3. ASSERT (Verificar: Debe obligar a completar el cierre pendiente antes de avanzar)
        assertEquals(DoubleShiftProcessor.MSG_ERROR_HARDWARE_SHUTDOWN, resultado);
    }

    // CP105: INTENTO DE ACCESO DESDE UN CELULAR DISTINTO (Sad Path)
    @Test
    public void cp105_debeBloquearAccesoSiLaUnidadYaRegistraTurnoAbiertoEnOtroTelefono() {
        // 1. ARRANGE (Preparar: Cuenta o bus se encuentra en uso activo por otra tripulación)
        boolean tieneTurnoActivoServidor = true;
        boolean sufrioApagadoImprevisto = false;

        // 2. ACT (Actuar: Intentar abrir sesión en paralelo en el segundo smartphone)
        String resultado = processor.evaluarDobleTurno(tieneTurnoActivoServidor, sufrioApagadoImprevisto);

        // 3. ASSERT (Verificar: El sistema debe interceptar el conflicto bloqueando la ventana)
        assertEquals(DoubleShiftProcessor.MSG_ERROR_DOUBLE_LOGIN, resultado);
    }
}