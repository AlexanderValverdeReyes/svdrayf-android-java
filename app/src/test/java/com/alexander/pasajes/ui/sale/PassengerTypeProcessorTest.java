package com.alexander.pasajes.ui.sale;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class PassengerTypeProcessorTest {

    private PassengerTypeProcessor processor;

    @Before
    public void setUp() {
        // Inicialización global del procesador de pasajes
        processor = new PassengerTypeProcessor();
    }

    // CP75: ESCENARIO EXITOSO — SELECCIÓN DE CATEGORÍA CONFORME (Happy Path)
    @Test
    public void cp75_debeAprobarSeleccionSiLosParaderosEstanMarcadosYEsDiaNormal() {
        // 1. ARRANGE (Preparar: Paraderos intermedios seleccionados en día de semana)
        int origenId = 1;
        int destinoId = 4;
        String tipoPasajero = "Adulto";
        String regimenDia = "NORMAL";

        // 2. ACT (Actuar: Simular la conmutación táctil del RadioButton)
        String resultado = processor.evaluarSeleccionPasajero(origenId, destinoId, tipoPasajero, regimenDia);

        // 3. ASSERT (Verificar: El sistema debe confirmar el estado para identificar el precio)
        assertEquals(PassengerTypeProcessor.STATUS_PASSENGER_OK, resultado);
    }

    // CP76: INTENTO DE SELECCIÓN SIN PARADEROS CONFIGURADOS (Sad Path)
    @Test
    public void cp76_debeBloquearCalculoYNotificarSiLosSelectoresEstanVacios() {
        // 1. ARRANGE (Preparar: Selectores de tramos en estado inicial -1)
        int origenId = -1;
        int destinoId = -1;
        String tipoPasajero = "Escolar";
        String regimenDia = "NORMAL";

        // 2. ACT (Actuar: Presionar directamente la categoría de pasajero)
        String resultado = processor.evaluarSeleccionPasajero(origenId, destinoId, tipoPasajero, regimenDia);

        // 3. ASSERT (Verificar: Debe forzar el despliegue de la glosa restrictiva de tramos)
        assertEquals(PassengerTypeProcessor.MSG_ERROR_MISSING_STOPS, resultado);
    }

    // CP77: TARIFA UNIVERSITARIA EN DÍAS NO LABORABLES O FERIADOS (Sad Path)
    @Test
    public void cp77_debeLanzarAlertaPreventivaSiSeAplicaMedioPasajeEnDomingo() {
        // 1. ARRANGE (Preparar: Reloj detecta operación en Domingo/Feriado y se marca Universitario)
        int origenId = 1;
        int destinoId = 2;
        String tipoPasajero = "Universitario";
        String regimenDia = "FERIADO";

        // 2. ACT (Actuar: Evaluar la vigencia del carné físico del estudiante)
        String resultado = processor.evaluarSeleccionPasajero(origenId, destinoId, tipoPasajero, regimenDia);

        // 3. ASSERT (Verificar: El núcleo debe exigir la confirmación del cobrador)
        assertEquals(PassengerTypeProcessor.MSG_WARN_UNIVERISTARIO_HOLIDAY, resultado);
    }
}