package com.alexander.pasajes.ui.sale;

import android.content.Context;
import android.util.Log;
import com.alexander.pasajes.data.entity.ConfiguracionEmpresa; // ← Importación conforme a tu entidad
import com.alexander.pasajes.repository.AppRepository;
import com.dantsu.escposprinter.EscPosPrinter;
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PrinterHelper {
    private static final String TAG = "PrinterHelper";

    // ACTUALIZADO: Se añade el parámetro 'String hash' al método de hardware
    public static void imprimirBoleto(Context ctx, String bus, String tipo, double precio,
                                      String metodo, String hash, BluetoothConnection connection) {
        try {
            AppRepository repo = new AppRepository(ctx);
            ConfiguracionEmpresa config = repo.getConfiguracion(); // ← Método puente conforme de tu repositorio

            String razonSocial = (config != null) ? config.razonSocial : "Turismo Kataleant";
            String ruc = (config != null) ? config.ruc : "20748596125";
            String direccion = (config != null) ? config.direccionFiscal : "Av. Escala Baja, Mala";
            String leyenda = (config != null) ? config.leyendaPie : "¡Gracias por confiar en el servicio de SVDRAYF!";

            EscPosPrinter printer = new EscPosPrinter(connection, 203, 48f, 32);
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());

            StringBuilder ticket = new StringBuilder();
            ticket.append("[C]<b>").append(razonSocial).append("</b>\n");
            ticket.append("[C]RUC: ").append(ruc).append("\n");
            ticket.append("[C]").append(direccion).append("\n");
            ticket.append("[C]--------------------------------\n");
            ticket.append("[L]<b>Unidad Bus:</b> ").append(bus).append("\n");
            ticket.append("[L]<b>Pasaje:</b> ").append(tipo).append("\n");
            ticket.append("[L]<b>Tarifa:</b> S/ ").append(String.format(Locale.US, "%.2f", precio)).append("\n");
            ticket.append("[L]<b>Metodo Pago:</b> ").append(metodo).append("\n"); // Muestra el texto (EFECTIVO / QR) según se realizó
            ticket.append("[L]<b>Emision:</b> ").append(sdf.format(new Date())).append("\n");
            ticket.append("[C]--------------------------------\n");

            //  MODIFICACIÓN CRÍTICA: Se remueve la imagen y se inyecta el Hash en texto plano legible
            ticket.append("[C]<b>CODIGO INSPECTOR:</b>\n");
            ticket.append("[C]<b>").append(hash).append("</b>\n");
            ticket.append("[C]--------------------------------\n");

            ticket.append("[C]").append(leyenda).append("\n");
            ticket.append("[L]\n\n\n\n");

            printer.printFormattedText(ticket.toString()); // ← Ejecución limpia sin parámetros adicionales
        } catch (Exception e) {
            Log.e(TAG, "Error en transmisión SPP Bluetooth: " + e.getMessage());
        }
    }
}