package com.alexander.pasajes.ui.printer;

import android.os.Handler;
import android.os.Looper;

public class PrinterBatteryManager {

    private final PrinterBatteryProcessor processor = new PrinterBatteryProcessor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean enlaceBluetoothActivo = false;
    private int porcentajeSimulado = 100; // Simulación decreciente conforme avanza el pasillo
    private boolean ejecutandoMonitoreo = false;
    private final OnBatteryStatusListener listener;
    private static final int INTERVALO_MONITOREO_MS = 10000; // Consulta cíclica cada 10 segundos

    public interface OnBatteryStatusListener {
        /**
         * Notificación asíncrona hacia la interfaz gráfica de usuario.
         * @param dictamen Glosa de error o confirmación de telemetría.
         * @param porcentaje Carga útil calculada.
         * @param permitirOperacion Flag booleano de negocio para bloquear o liberar botones.
         */
        void onBatteryUpdate(String dictamen, int porcentaje, boolean permitirOperacion);
    }

    public PrinterBatteryManager(OnBatteryStatusListener listener) {
        this.listener = listener;
    }

    public void setEnlaceBluetoothActivo(boolean activo) {
        this.enlaceBluetoothActivo = activo;
    }

    public void iniciarMonitoreo() {
        if (ejecutandoMonitoreo) return;
        ejecutandoMonitoreo = true;
        bucleTelemetria.run();
    }

    public void detenerMonitoreo() {
        ejecutandoMonitoreo = false;
        handler.removeCallbacks(bucleTelemetria);
    }

    private final Runnable bucleTelemetria = new Runnable() {
        @Override
        public void run() {
            if (!ejecutandoMonitoreo) return;

            // Desgaste natural simulado de la batería en ruta para testing físico
            if (enlaceBluetoothActivo && porcentajeSimulado > 5) {
                porcentajeSimulado -= 1;
            }

            // Transformamos el porcentaje entero a un array de bytes emulando la respuesta ESC/POS
            byte[] rawBytes = new byte[]{(byte) porcentajeSimulado};

            // Evaluamos las reglas de negocio de tu procesador desacoplado
            String dictamen = processor.evaluarMonitoreoBateria(enlaceBluetoothActivo, rawBytes);
            boolean permitirOperacion = PrinterBatteryProcessor.STATUS_TELEMETRY_OK.equals(dictamen);

            if (listener != null) {
                listener.onBatteryUpdate(dictamen, porcentajeSimulado, permitirOperacion);
            }

            // Planificamos el siguiente ciclo periódico de escaneo
            handler.postDelayed(this, INTERVALO_MONITOREO_MS);
        }
    };

    // Método de contingencia para inyectar fallas o cargas críticas desde tus JUnit Tests
    public void forzarPorcentajeBateria(int porcentaje) {
        this.porcentajeSimulado = porcentaje;
    }
}