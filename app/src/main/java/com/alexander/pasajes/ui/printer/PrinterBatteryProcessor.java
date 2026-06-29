package com.alexander.pasajes.ui.printer;

public class PrinterBatteryProcessor {

    public static final String STATUS_TELEMETRY_OK = "TELEMETRIA_CONFORME";
    public static final String MSG_ERROR_DISCONNECTED = "Alerta de Hardware: Conexión con la miniticketera térmica interrumpida. Verifique que el dispositivo periférico esté encendido y dentro del rango Bluetooth antes de continuar con la venta en efectivo.";
    public static final String MSG_ERROR_CRITICAL_BATTERY = "Recarga Obligatoria: Batería de impresora a nivel crítico (Menor al 10%). Conecte el cable de alimentación vehicular de 12V del bus de inmediato para restablecer la emisión física de comprobantes.";
    public static final String MSG_ERROR_CORRUPTED_DATA = "Error de Telemetría: Trama de energía corrupta o no reconocida por el hardware ESC/POS.";

    /**
     * Retorna la secuencia binaria de escape 'DLE EOT 6' (0x10, 0x04, 0x06).
     */
    public byte[] obtenerComandoLecturaBateria() {
        return new byte[]{0x10, 0x04, 0x06};
    }

    /**
     * Mapea lecturas decimales directas y tramas de registros de hardware.
     */
    public int extraerPorcentajeDesdeBytes(byte[] rawBytes) {
        if (rawBytes == null || rawBytes.length == 0) {
            return -1;
        }

        int statusByte = rawBytes[0] & 0xFF;

        // Caso 1: Lectura decimal directa de batería (0 a 100)
        if (statusByte >= 0 && statusByte <= 100) {
            return statusByte;
        }

        // Caso 2: Máscara de bits de registros de hardware ESC/POS (Valores > 100)
        int bitsEnergia = (statusByte & 0x0C) >> 2;
        switch (bitsEnergia) {
            case 0: return 100; // Bits 00 -> Energía Completa
            case 1: return 50;  // Bits 01 -> Energía Media
            case 2: return 15;  // Bits 10 -> Energía Baja
            case 3: return 5;   // Bits 11 -> Nivel Crítico (5%)
            default: return -1;
        }
    }

    public String evaluarMonitoreoBateria(boolean enlaceBluetoothActivo, byte[] rawBytes) {
        if (!enlaceBluetoothActivo) {
            return MSG_ERROR_DISCONNECTED;
        }

        int porcentajeReal = extraerPorcentajeDesdeBytes(rawBytes);

        if (porcentajeReal == -1) {
            return MSG_ERROR_CORRUPTED_DATA;
        }

        if (porcentajeReal < 10) {
            return MSG_ERROR_CRITICAL_BATTERY;
        }

        return STATUS_TELEMETRY_OK;
    }
}