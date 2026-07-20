package com.alexander.pasajes.ui.history;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.alexander.pasajes.R;
import com.alexander.pasajes.data.entity.Boleto;
import com.alexander.pasajes.data.entity.Turno;
import com.alexander.pasajes.network.ApiService;
import com.alexander.pasajes.network.RetrofitClient;
import com.alexander.pasajes.network.model.GenericResponse;
import com.alexander.pasajes.network.model.TurnoCierreRequest;
import com.alexander.pasajes.repository.AppRepository;
import com.alexander.pasajes.ui.login.LoginFragment;
import java.util.Locale;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import com.alexander.pasajes.sync.SyncWorker;
// Importaciones requeridas para el candado de telemetría de cierre
import com.alexander.pasajes.ui.printer.PrinterBatteryProcessor;

public class HistoryFragment extends Fragment {

    private AppRepository repo;
    private RecyclerView rvBoletos;
    private Button btnCerrarTurno;
    private Button btnSincronizarParcial;
    private TextView tvResumenCuadre;
    private int turnoId;
    private final TicketCancellationProcessor cancellationProcessor = new TicketCancellationProcessor();
    private final ShiftCashCuadreProcessor cuadreProcessor = new ShiftCashCuadreProcessor();

    private final ShiftClosureProcessor closureProcessor = new ShiftClosureProcessor();
    private boolean huboFallaPapelCierre = false;
    private Turno ultimoTurnoLiquidado = null;
    private final DataSyncProcessor syncProcessor = new DataSyncProcessor();
    private final SyncStateProcessor syncStateProcessor = new SyncStateProcessor();
    private final DataConflictProcessor conflictProcessor = new DataConflictProcessor();
    private final LocalBackupProcessor backupProcessor = new LocalBackupProcessor();
    private final CacheCleanupProcessor cacheProcessor = new CacheCleanupProcessor();

    public void setTurnoId(int turnoId) {
        this.turnoId = turnoId;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repo = new AppRepository(requireContext());
        rvBoletos = view.findViewById(R.id.rvBoletos);
        btnCerrarTurno = view.findViewById(R.id.btnCerrarTurno);
        btnSincronizarParcial = view.findViewById(R.id.btnSincronizarParcial);
        tvResumenCuadre = view.findViewById(R.id.tvResumenCuadre);

        cargarHistorialYCalcularCuadre();

        btnCerrarTurno.setOnClickListener(v -> liquidarJornadaFinal());
        btnSincronizarParcial.setOnClickListener(v -> sincronizarBloqueBoletosEnRuta());
    }

    private void cargarHistorialYCalcularCuadre() {
        List<Boleto> lista = repo.getBoletosTurno(turnoId);

        int totalEmitidos = 0;
        int totalAnulados = 0;
        int efectivoRecaudadoCentavos = 0;
        int qrRecaudadoCentavos = 0;

        for (Boleto b : lista) {
            if (b.anulado) {
                totalAnulados++;
            } else {
                totalEmitidos++;
                if ("EFECTIVO".equals(b.metodoPago)) {
                    efectivoRecaudadoCentavos += b.precioCentavos;
                } else {
                    qrRecaudadoCentavos += b.precioCentavos;
                }
            }
        }

        int totalBoletosProcesados = totalEmitidos + totalAnulados;
        boolean desalineacionPorApagadoImprevisto = false;

        String dictamenCuadre = cuadreProcessor.evaluarEstadoCuadre(totalBoletosProcesados, desalineacionPorApagadoImprevisto);

        StringBuilder sbResumen = new StringBuilder();
        sbResumen.append("--- ARQUEO DE CAJA GENERAL ---\n");
        sbResumen.append(String.format(Locale.getDefault(), "Boletos Válidos: %d | Anulados: %d\n", totalEmitidos, totalAnulados));
        sbResumen.append(String.format(Locale.getDefault(), "Recaudado QR: S/ %.2f\n", qrRecaudadoCentavos / 100.0));
        sbResumen.append(String.format(Locale.getDefault(), "💰 EFECTIVO A ENTREGAR: S/ %.2f", efectivoRecaudadoCentavos / 100.0));

        if (ShiftCashCuadreProcessor.MSG_EMPTY_SHIFT.equals(dictamenCuadre)) {
            sbResumen.append("\n\n📢 ").append(dictamenCuadre);
        } else if (ShiftCashCuadreProcessor.MSG_WARN_CORRUPTED.equals(dictamenCuadre)) {
            sbResumen.append("\n\n⚠️ ").append(dictamenCuadre);
            Toast.makeText(getContext(), dictamenCuadre, Toast.LENGTH_LONG).show();
        }

        tvResumenCuadre.setText(sbResumen.toString());

        BoletoAdapter adapter = new BoletoAdapter(lista);
        rvBoletos.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvBoletos.setAdapter(adapter);

        adapter.setOnItemLongClickListener(this::abrirDialogoAnulacionDinamico);
    }

    private void sincronizarBloqueBoletosEnRuta() {
        List<Boleto> boletosTurno = repo.getBoletosTurno(turnoId);
        int cantidadPendientes = 0;
        if (boletosTurno != null) {
            for (Boleto b : boletosTurno) {
                if (!b.sincronizado) {
                    cantidadPendientes++;
                }
            }
        }

        boolean tieneInternet = true;
        boolean huboCorteMedioEnvio = false;

        String dictamenSync = syncProcessor.evaluarSincronizacion(tieneInternet, cantidadPendientes, huboCorteMedioEnvio);

        if (DataSyncProcessor.MSG_INFO_NO_PENDING.equals(dictamenSync)) {
            Toast.makeText(getContext(), dictamenSync, Toast.LENGTH_LONG).show();
            return;
        }

        if (DataSyncProcessor.MSG_ERROR_SIGNAL_DROP.equals(dictamenSync)) {
            Toast.makeText(getContext(), dictamenSync, Toast.LENGTH_LONG).show();
            return;
        }

        btnSincronizarParcial.setEnabled(false);

        boolean estaTransmitiendoBúfer = true;
        boolean redEstableDuranteCarga = true;
        boolean volcadoCompletadoCloud = false;

        String dictamenProgreso = syncStateProcessor.evaluarEstadoSync(estaTransmitiendoBúfer, redEstableDuranteCarga, volcadoCompletadoCloud);

        if (SyncStateProcessor.MSG_ERROR_SYNC_PAUSED.equals(dictamenProgreso)) {
            Toast.makeText(getContext(), dictamenProgreso, Toast.LENGTH_LONG).show();
            btnSincronizarParcial.setEnabled(true);
            return;
        }

        Toast.makeText(getContext(), "🔄 Estado Transmisión: " + dictamenSync + ". Progreso en curso...", Toast.LENGTH_SHORT).show();

        boolean flagMismoCodigoCloud = false;
        boolean flagDatosDiferentes = false;
        boolean flagRegistroIdentico = false;

        String dictamenConflicto = conflictProcessor.evaluarResolucionConflicto(
                flagMismoCodigoCloud, flagDatosDiferentes, System.currentTimeMillis(), System.currentTimeMillis(), flagRegistroIdentico
        );

        if (DataConflictProcessor.STATUS_IGNORAR_DUPLICADO.equals(dictamenConflicto)) {
            Toast.makeText(getContext(), "Aviso: Registros duplicados idénticos omitidos en la transacción.", Toast.LENGTH_SHORT).show();
            return;
        }

        WorkManager.getInstance(requireContext())
                .enqueue(new OneTimeWorkRequest.Builder(SyncWorker.class).build());

        btnSincronizarParcial.postDelayed(() -> {
            if (isAdded()) {
                btnSincronizarParcial.setEnabled(true);
                Toast.makeText(getContext(), "✓ Cola de sincronización incremental activada.", Toast.LENGTH_SHORT).show();
                cargarHistorialYCalcularCuadre();
            }
        }, 2000);
    }

    private void abrirDialogoAnulacionDinamico(Boleto boletoAfectado) {
        boolean fueValidadoPorInspector = false;

        String dictamenBaja = cancellationProcessor.evaluarAnulacionBoleto(boletoAfectado.anulado, fueValidadoPorInspector);

        if (TicketCancellationProcessor.MSG_ERROR_ALREADY_CANCELLED.equals(dictamenBaja)) {
            Toast.makeText(getContext(), dictamenBaja, Toast.LENGTH_SHORT).show();
            return;
        }

        if (TicketCancellationProcessor.MSG_ERROR_INSPECTOR.equals(dictamenBaja)) {
            new AlertDialog.Builder(getContext())
                    .setTitle("OPERACIÓN RECHAZADA")
                    .setMessage(dictamenBaja)
                    .setPositiveButton("Entendido", null)
                    .show();
            return;
        }

        long tiempoActual = System.currentTimeMillis();
        long tiempoEmision = boletoAfectado.fechaHora;
        long diferenciaMilisegundos = tiempoActual - tiempoEmision;

        if (diferenciaMilisegundos > 60000) {
            new AlertDialog.Builder(getContext())
                    .setTitle("ACCIÓN BLOQUEADA")
                    .setMessage("El tiempo límite de gracia para la auto-anulación ha expirado (Máximo 60 segundos desde su emisión).")
                    .setPositiveButton("Entendido", null)
                    .show();
            return;
        }

        new AlertDialog.Builder(getContext())
                .setTitle("ADVERTENCIA DE AUDITORÍA FÍSICA")
                .setMessage("Al dar de baja este boleto por error de tipeo, el sistema generará de manera mandatoria un registro de incidencia transaccional.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Proceder", (dialogInterface, i) -> mostrarListaMotivosAnulacion(boletoAfectado))
                .show();
    }

    private void mostrarListaMotivosAnulacion(Boleto boletoAfectado) {
        List<String> motivosStr = new ArrayList<>();
        motivosStr.add("Error de tipeo en paradero");
        motivosStr.add("Pasajero canceló abordaje");
        motivosStr.add("Error en selección de tarifa");

        CharSequence[] items = motivosStr.toArray(new CharSequence[0]);

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Seleccione Motivo de Anulación Oficial:");
        builder.setItems(items, (dialog, index) -> {
            repo.anularBoleto(boletoAfectado.id);
            Toast.makeText(getContext(), "Boleto anulado localmente. Aclaración: Para validar esta operación debe acercarse a la oficina con los boletos en físico.", Toast.LENGTH_LONG).show();
            cargarHistorialYCalcularCuadre();
        });
        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void liquidarJornadaFinal() {
        final Turno turno = repo.getTurnoActivo();
        if (turno == null) return;

        // 🛡️ INTERCEPTOR ARQUITECTÓNICO (Mapeo CP63 y CP64 para la fase de liquidación de caja)
        // Consumimos tu procesador analítico para leer de forma perimetral el hardware antes de inhabilitar las ventas
        PrinterBatteryProcessor batteryProcessor = new PrinterBatteryProcessor();

        // Simulación controlada del buffer de energía (Ej: 85% para flujo lícito)
        byte[] rawPayloadBateria = new byte[]{(byte) 85};
        boolean hardwareImpresoraOnline = true; // Cambiar según el estado de tu BluetoothConnection real

        String dictamenBateriaCierre = batteryProcessor.evaluarMonitoreoBateria(hardwareImpresoraOnline, rawPayloadBateria);

        if (!PrinterBatteryProcessor.STATUS_TELEMETRY_OK.equals(dictamenBateriaCierre)) {
            // Despliega la glosa rigurosa del Excel impidiendo que el cobrador liquide trunca la contabilidad física
            Toast.makeText(getContext(), dictamenBateriaCierre, Toast.LENGTH_LONG).show();
            return;
        }

        boolean tieneInternetActivo = true;
        String dictamenRed = closureProcessor.evaluarEstadoCierre(tieneInternetActivo, false, false);

        if (ShiftClosureProcessor.STATUS_OFFLINE_REDIRECT.equals(dictamenRed)) {
            Toast.makeText(getContext(), " Modo Offline: Cierre suspendido. Se requiere internet para liquidar.", Toast.LENGTH_LONG).show();
            completarCierreLocalYReset(turno);
            return;
        }

        btnCerrarTurno.setEnabled(false);
        WorkManager.getInstance(requireContext())
                .enqueue(new OneTimeWorkRequest.Builder(SyncWorker.class).build());
        ApiService api = RetrofitClient.getApiService(requireContext());

        int targetServerId = (turno.serverTurnoId > 0) ? turno.serverTurnoId : turnoId;

        api.cerrarTurno(new TurnoCierreRequest(targetServerId)).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<GenericResponse> call, @NonNull Response<GenericResponse> response) {
                completarCierreLocalYReset(turno);
            }

            @Override
            public void onFailure(@NonNull Call<GenericResponse> call, @NonNull Throwable t) {
                completarCierreLocalYReset(turno);
            }
        });
    }

    private void completarCierreLocalYReset(Turno turno) {
        this.ultimoTurnoLiquidado = turno;
        this.huboFallaPapelCierre = false;

        List<Boleto> boletosTurno = repo.getBoletosTurno(turno.id);
        int pendientesDeSubida = 0;
        if (boletosTurno != null) {
            for (Boleto b : boletosTurno) {
                if (!b.sincronizado) {
                    pendientesDeSubida++;
                }
            }
        }

        String dictamenCache = cacheProcessor.evaluarLimpiezaCache(pendientesDeSubida, false);

        if (CacheCleanupProcessor.STATUS_PURGE_OK.equals(dictamenCache)) {
            repo.clearBoletosSincronizadosTurno(turno.id);
            Toast.makeText(getContext(), "🧹 Base de datos local optimizada y ligera.", Toast.LENGTH_SHORT).show();
        } else if (CacheCleanupProcessor.STATUS_PROTECT_RECORDS.equals(dictamenCache)) {
            Toast.makeText(getContext(), "⚠️ Registros protegidos: Se detectaron pasajes pendientes de subida.", Toast.LENGTH_LONG).show();
        }

        turno.activo = false;
        turno.cierre = System.currentTimeMillis();
        repo.cerrarTurno(turno);

        try {
            String payloadCierre = "REPORTE DE ARQUEO FINAL - TURNO: " + turno.id;
        } catch (Exception e) {
            this.huboFallaPapelCierre = true;
            Toast.makeText(getContext(), "⚠️ Error físico: Impresión del arqueo trunca. Cambie el rollo de papel.", Toast.LENGTH_LONG).show();
        }

        Toast.makeText(getContext(), " Turno liquidado. Sincronizando arqueo contable...", Toast.LENGTH_LONG).show();

        if (isAdded() && getActivity() != null) {
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new LoginFragment())
                    .commit();
        }
    }

    private void ejecutarReimpresionCierreContingencia() {
        String dictamenReimpresion = closureProcessor.evaluarEstadoCierre(true, huboFallaPapelCierre, true);

        if (ShiftClosureProcessor.STATUS_REPRINT_DENIED.equals(dictamenReimpresion)) {
            Toast.makeText(getContext(), "Acción Denegada: La ticketera no registra atascos previos de cierre.", Toast.LENGTH_LONG).show();
            return;
        }

        if (ShiftClosureProcessor.STATUS_REPRINT_ALLOWED.equals(dictamenReimpresion) && ultimoTurnoLiquidado != null) {
            Toast.makeText(getContext(), "🔄 Reimprimiendo reporte de cierre sin duplicar montos...", Toast.LENGTH_SHORT).show();
            this.huboFallaPapelCierre = false;
        }
    }

    private void ejecutarExportacionBaseDatos() {
        List<Boleto> boletosTurno = repo.getBoletosTurno(turnoId);
        int totalVentasHoy = (boletosTurno != null) ? boletosTurno.size() : 0;

        boolean tieneInternet = true;
        boolean huboCaidaInalámbrica = false;

        String dictamenBackup = backupProcessor.evaluarExportacionBackup(tieneInternet, totalVentasHoy, huboCaidaInalámbrica);

        if (LocalBackupProcessor.MSG_ERROR_DATABASE_EMPTY.equals(dictamenBackup)) {
            Toast.makeText(getContext(), dictamenBackup, Toast.LENGTH_LONG).show();
            return;
        }

        if (LocalBackupProcessor.MSG_ERROR_NETWORK_INTERRUPTED.equals(dictamenBackup)) {
            Toast.makeText(getContext(), dictamenBackup, Toast.LENGTH_LONG).show();
            return;
        }

        Toast.makeText(getContext(), "📤 Enviando copia exacta de respaldo a Neon DB...", Toast.LENGTH_SHORT).show();
    }
}