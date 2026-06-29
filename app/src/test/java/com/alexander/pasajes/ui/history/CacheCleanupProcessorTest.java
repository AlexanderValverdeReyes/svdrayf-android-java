package com.alexander.pasajes.ui.history;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class CacheCleanupProcessorTest {

    private CacheCleanupProcessor processor;

    @Before
    public void setUp() {
        // Inicialización de la máquina de depuración de memoria RAM/Caché
        processor = new CacheCleanupProcessor();
    }

    // CP142: ESCENARIO EXITOSO — DEPURACIÓN CORRECTA DE PASAJES ANTIGUOS (Happy Path)
    @Test
    public void cp142_debeLimpiarCacheLocalSiTodosLosBoletosFueronCopiadosEnLaNube() {
        // 1. ARRANGE (Preparar: Turno cerrado con 0 boletos pendientes de subir)
        int boletosPendientes = 0;
        boolean turnoActivo = false;

        // 2. ACT (Actuar: Solicitar al procesador ejecutar la rutina de desborde)
        String resultado = processor.evaluarLimpiezaCache(boletosPendientes, turnoActivo);

        // 3. ASSERT (Verificar: Debe ordenar la purga de SQLite para mantener la app ligera)
        assertEquals(CacheCleanupProcessor.STATUS_PURGE_OK, resultado);
    }

    // CP143: ERROR — BOLETOS ANTIGUOS PENDIENTES DE SINCRONIZACIÓN (Sad Path)
    @Test
    public void cp143_debeOmitirElBorradoYProtegerRegistrosSiFaltaSincronizarVentas() {
        // 1. ARRANGE (Preparar: Quedan 4 pasajes en cola debido a cortes en la carretera)
        int boletosPendientes = 4;
        boolean turnoActivo = false;

        // 2. ACT (Actuar: Intentar ejecutar la rutina de vaciado de caché local)
        String resultado = processor.evaluarLimpiezaCache(boletosPendientes, turnoActivo);

        // 3. ASSERT (Verificar: El sistema debe bloquear el borrado protegiendo la información de la empresa)
        assertEquals(CacheCleanupProcessor.STATUS_PROTECT_RECORDS, resultado);
    }

    // CP143: EXCEPCIÓN — RE-DIRECCIÓN POR APAGADO DE APLICACIÓN BRUPTO (Sad Path)
    @Test
    public void cp143_debeForzarRedireccionAPantallaDeVentaSiElTurnoQuedoAbierto() {
        // 1. ARRANGE (Preparar: App se cierra abruptamente dejando el viaje en estado activo)
        int boletosPendientes = 2;
        boolean turnoActivo = true;

        // 2. ACT (Actuar: Levantar la aplicación de nuevo desde el launcher)
        String resultado = processor.evaluarLimpiezaCache(boletosPendientes, turnoActivo);

        // 3. ASSERT (Verificar: Debe forzar el retorno inmediato al pasillo de ventas para cerrar el turno)
        assertEquals(CacheCleanupProcessor.STATUS_REDIRECT_ACTIVE, resultado);
    }
}