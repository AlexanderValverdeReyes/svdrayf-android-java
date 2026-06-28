package com.alexander.pasajes.sync;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.alexander.pasajes.data.entity.Boleto;
import com.alexander.pasajes.data.entity.Turno;
import com.alexander.pasajes.network.ApiService;
import com.alexander.pasajes.network.RetrofitClient;
import com.alexander.pasajes.network.model.GenericResponse;
import com.alexander.pasajes.network.model.SyncBoletosRequest;
import com.alexander.pasajes.network.model.BoletoSync;
import com.alexander.pasajes.repository.AppRepository;
import retrofit2.Response;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SyncWorker extends Worker {
    private static final String TAG = "SyncWorker";

    public SyncWorker(@NonNull android.content.Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        AppRepository repo = new AppRepository(getApplicationContext());
        List<Boleto> noSync = repo.getBoletosNoSincronizados();

        if (noSync == null || noSync.isEmpty()) {
            Log.d(TAG, "No hay boletos pendientes por sincronizar.");
            return Result.success();
        }

        List<BoletoSync> listaDto = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);

        for (Boleto b : noSync) {
            BoletoSync dto = new BoletoSync();

            // 🚀 IMPLEMENTACIÓN DE TU IDEA: Usamos el UUID v4 único y aleatorio generado en la venta
            dto.id_boleto = b.uuid;

            // Mapeo dinámico del Turno serializado de la nube
            Turno turnoLocal = repo.getTurnoPorId(b.turnoId);
            if (turnoLocal != null && turnoLocal.serverTurnoId > 0) {
                dto.id_turno = turnoLocal.serverTurnoId;
                Log.d(TAG, "Mapeando boleto uuid " + b.uuid + " al id_turno real de la nube: " + turnoLocal.serverTurnoId);
            } else {
                dto.id_turno = b.turnoId;
            }

            // 🚀 IMPLEMENTACIÓN DE TU IDEA: Enviamos el ID real de la tarifa capturada dinámicamente
            dto.id_tarifario = b.tarifarioId;

            dto.monto_pagado_centavos = b.precioCentavos;
            dto.modalidad_pago = b.metodoPago != null ? b.metodoPago : "EFECTIVO";
            dto.estado_boleto = b.anulado ? "ANULADO" : "VALIDO";
            dto.hash_qr = b.hash != null ? b.hash : "ERR-HASH";
            dto.alerta_auditoria_qr = b.huboIntentoQR;

            dto.fecha_emision = sdf.format(new Date(b.fechaHora));
            dto.es_reimpresion = false;
            dto.id_boleto_original = null;

            if (b.anulado) {
                dto.id_motivo_anulacion = 1;
                dto.fecha_anulacion = sdf.format(new Date());
            } else {
                dto.id_motivo_anulacion = null;
                dto.fecha_anulacion = null;
            }

            listaDto.add(dto);
        }

        ApiService api = RetrofitClient.getApiService(getApplicationContext());
        SyncBoletosRequest request = new SyncBoletosRequest(listaDto);

        try {
            Response<GenericResponse> response = api.syncBoletos(request).execute();

            if (response.isSuccessful() && response.body() != null) {
                for (Boleto b : noSync) {
                    repo.marcarSincronizado(b.id);
                }
                Log.d(TAG, "✅ Lote de " + noSync.size() + " transacciones sincronizado con Neon DB.");
                return Result.success();
            } else {
                String detalleErrorBackend = "No se pudo extraer el cuerpo del error";
                if (response.errorBody() != null) {
                    detalleErrorBackend = response.errorBody().string();
                }

                Log.e(TAG, "❌ El servidor rechazó el lote. Código HTTP: " + response.code());
                Log.e(TAG, "📝 AUDITORÍA FORENSE POSTGRESQL: " + detalleErrorBackend);

                return Result.retry();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error crítico de cobertura en carretera: " + e.getMessage());
            return Result.retry();
        }
    }
}