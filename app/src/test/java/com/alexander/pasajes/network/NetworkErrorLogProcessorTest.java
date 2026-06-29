package com.alexander.pasajes.network;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class NetworkErrorLogProcessorTest {

    private NetworkErrorLogProcessor processor;

    @Before
    public void setUp() {
        // Inicialización del motor analítico de registro de incidencias técnicas
        processor = new NetworkErrorLogProcessor();
    }

    // CP135: ESCENARIO EXITOSO — REGISTRO CONFORME DE FALLAS DE RED (Happy Path)
    @Test
    public void cp135_debeCapturarYConfirmarElRegistroDeLaFallaConSuHoraExacta() {
        // 1. ARRANGE (Preparar: Falla imprevista de timeout de red y timestamp actual)
        String codigoFallo = "TIMEOUT_CONNECT_EXCEPTION";
        long horaExacta = System.currentTimeMillis();

        // 2. ACT (Actuar: Enviar los detalles capturados al procesador de red)
        String resultado = processor.registrarFallaTransmision(codigoFallo, horaExacta);

        // 3. ASSERT (Verificar: El sistema debe dar conformidad para el guardado local del log)
        assertEquals(NetworkErrorLogProcessor.STATUS_LOGGED_OK, resultado);
    }

    // CP136: ERROR — CONSULTA DE LISTA SIN FALLAS REGISTRADAS (Sad Path)
    @Test
    public void cp136_debeRetornarMensajeInformativoMandatorioSiNoHayFallasEnLaJornada() {
        // 1. ARRANGE (Preparar: Aplicativo opera sin interrupciones, la lista de Room tiene 0 registros)
        int cantidadErrores = 0;

        // 2. ACT (Actuar: El Administrador ingresa al menú técnico de "Log de Errores")
        String resultado = processor.evaluarConsultaListaErrores(cantidadErrores);

        // 3. ASSERT (Verificar: Debe retornar de forma estricta la glosa informativa exigida por el caso)
        assertEquals(NetworkErrorLogProcessor.MSG_NO_ERRORS, resultado);
    }
}