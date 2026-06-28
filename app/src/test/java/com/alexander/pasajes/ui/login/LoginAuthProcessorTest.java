package com.alexander.pasajes.ui.login; // 🟢 Paquete correcto en la sección de pruebas

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class LoginAuthProcessorTest {

    private LoginAuthProcessor processor;

    @Before
    public void setUp() {
        processor = new LoginAuthProcessor();
    }

    @Test
    public void cp54_debeAutorizarInicioSesionOnlineConCredencialesCorrectas() {
        // 1. ARRANGE
        boolean esOnline = true;
        boolean apiSuccess = true;
        boolean usuarioLocalEncontrado = false;
        boolean baseDatosLocalVacia = true;

        // 2. ACT
        String resultado = processor.evaluarEstadoAutenticacion(esOnline, apiSuccess, usuarioLocalEncontrado, baseDatosLocalVacia);

        // 3. ASSERT
        assertEquals(LoginAuthProcessor.MSG_SUCCESS_ONLINE, resultado);
    }

    @Test
    public void cp55_debeAutorizarModoContingenciaSiExisteHistorialLocal() {
        // 1. ARRANGE
        boolean esOnline = false;
        boolean apiSuccess = false;
        boolean usuarioLocalEncontrado = true;
        boolean baseDatosLocalVacia = false;

        // 2. ACT
        String resultado = processor.evaluarEstadoAutenticacion(esOnline, apiSuccess, usuarioLocalEncontrado, baseDatosLocalVacia);

        // 3. ASSERT
        assertEquals(LoginAuthProcessor.MSG_SUCCESS_OFFLINE, resultado);
    }

    @Test
    public void cp56_debeRechazarAccesoYMostrarMensajeExplicitoSiLasCredencialesFallan() {
        // 1. ARRANGE
        boolean esOnline = true;
        boolean apiSuccess = false;
        boolean usuarioLocalEncontrado = false;
        boolean baseDatosLocalVacia = false;

        // 2. ACT
        String resultado = processor.evaluarEstadoAutenticacion(esOnline, apiSuccess, usuarioLocalEncontrado, baseDatosLocalVacia);

        // 3. ASSERT
        assertEquals(LoginAuthProcessor.MSG_ERROR_CREDENTIALS, resultado);
    }

    @Test
    public void cp57_debeBloquearAccesoOfflineSiLaBaseDatosRoomEstaCompletamenteVacia() {
        // 1. ARRANGE
        boolean esOnline = false;
        boolean apiSuccess = false;
        boolean usuarioLocalEncontrado = false;
        boolean baseDatosLocalVacia = true;

        // 2. ACT
        String resultado = processor.evaluarEstadoAutenticacion(esOnline, apiSuccess, usuarioLocalEncontrado, baseDatosLocalVacia);

        // 3. ASSERT
        assertEquals(LoginAuthProcessor.MSG_ERROR_NO_SYNC, resultado);
    }
}