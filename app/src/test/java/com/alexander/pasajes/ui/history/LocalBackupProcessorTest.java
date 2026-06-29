package com.alexander.pasajes.ui.history;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class LocalBackupProcessorTest {

    private LocalBackupProcessor processor;

    @Before
    public void setUp() {
        // Inicialización del motor analítico de copias de seguridad
        processor = new LocalBackupProcessor();
    }

    // CP139: ESCENARIO EXITOSO — EXPORTACIÓN CONFORME DE BASE DE DATOS (Happy Path)
    @Test
    public void cp139_debeIniciarExportacionSiElTerminalTieneInternetYVentasRegistradas() {
        // 1. ARRANGE (Preparar: Internet estable y terminal con 15 operaciones realizadas)
        boolean tieneInternet = true;
        int cantidadVentasRegistradas = 15;
        boolean corteSenalMedioEnvio = false;

        // 2. ACT (Actuar: Solicitar al procesador la viabilidad del respaldo)
        String resultado = processor.evaluarExportacionBackup(tieneInternet, cantidadVentasRegistradas, corteSenalMedioEnvio);

        // 3. ASSERT (Verificar: Debe otorgar conformidad para streamear el archivo SQLite)
        assertEquals(LocalBackupProcessor.STATUS_BACKUP_OK, resultado);
    }

    // CP140: ERROR — CAÍDA DE INTERNET DURANTE EL ENVÍO (Sad Path)
    @Test
    public void cp140_debeInterrumpirYProtegerArchivoOriginalSiSeCortaLaSenalAMedias() {
        // 1. ARRANGE (Preparar: Red celular se corta abruptamente a mitad de la subida del binario)
        boolean tieneInternet = true;
        int cantidadVentasRegistradas = 10;
        boolean corteSenalMedioEnvio = true; // Radiofrecuencia interrumpida

        // 2. ACT (Actuar: Capturar la interrupción del socket web)
        String resultado = processor.evaluarExportacionBackup(tieneInternet, cantidadVentasRegistradas, corteSenalMedioEnvio);

        // 3. ASSERT (Verificar: Debe forzar la detención arrojando la alerta exacta del Excel)
        assertEquals(LocalBackupProcessor.MSG_ERROR_NETWORK_INTERRUPTED, resultado);
    }

    // CP141: ERROR — INTENTO DE EXPORTAR BASE DE DATOS SIN OPERACIONES (Sad Path)
    @Test
    public void cp141_debeCancelarAccionSiLaBaseDeDatosLocalSeEncuentraVacia() {
        // 1. ARRANGE (Preparar: Aplicación recién instalada, base de datos local en 0 filas)
        boolean tieneInternet = true;
        int cantidadVentasRegistradas = 0;
        boolean corteSenalMedioEnvio = false;

        // 2. ACT (Actuar: Cobrador presiona el botón "Exportar Base de Datos")
        String resultado = processor.evaluarExportacionBackup(tieneInternet, cantidadVentasRegistradas, corteSenalMedioEnvio);

        // 3. ASSERT (Verificar: Debe abortar de inmediato con la glosa restrictiva mandatoria)
        assertEquals(LocalBackupProcessor.MSG_ERROR_DATABASE_EMPTY, resultado);
    }
}