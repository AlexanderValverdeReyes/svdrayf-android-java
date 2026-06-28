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

public class HistoryFragment extends Fragment {

    private AppRepository repo;
    private RecyclerView rvBoletos;
    private Button btnCerrarTurno;

    // ADICIÓN: Nuevo botón para subir datos oportunamente en zonas con internet (Ej: Chilca)
    private Button btnSincronizarParcial;

    private TextView tvResumenCuadre;
    private int turnoId;

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

        //ENLACE DEL NUEVO BOTÓN: Sincroniza datos sin cerrar la jornada de venta
        btnSincronizarParcial = view.findViewById(R.id.btnSincronizarParcial);

        tvResumenCuadre = view.findViewById(R.id.tvResumenCuadre);

        cargarHistorialYCalcularCuadre();

        btnCerrarTurno.setOnClickListener(v -> liquidarJornadaFinal());

        // Asignación del disparador asíncrono intermedio
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

        tvResumenCuadre.setText(String.format(Locale.getDefault(),
                "--- ARQUEO DE CAJA GENERAL ---\nBoletos Válidos: %d | Anulados: %d\nRecaudado QR: S/ %.2f\n💰 EFECTIVO A ENTREGAR: S/ %.2f",
                totalEmitidos, totalAnulados, qrRecaudadoCentavos / 100.0, efectivoRecaudadoCentavos / 100.0));

        BoletoAdapter adapter = new BoletoAdapter(lista);
        rvBoletos.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvBoletos.setAdapter(adapter);

        adapter.setOnItemLongClickListener(this::abrirDialogoAnulacionDinamico);
    }

    // NUEVA FUNCIONALIDAD: Dispara la sincronización en segundo plano manteniendo el turno ABIERTO
    private void sincronizarBloqueBoletosEnRuta() {
        btnSincronizarParcial.setEnabled(false);
        Toast.makeText(getContext(), "🔄 Sincronizando bloque de boletos con Neon DB...", Toast.LENGTH_SHORT).show();

        // Ejecuta el SyncWorker de forma inmediata. Room filtrará solo los boletos con 'sincronizado = false'.
        // Al subirse con éxito, pasarán a 'true' localmente, garantizando que no se vuelvan a subir.
        WorkManager.getInstance(requireContext())
                .enqueue(new OneTimeWorkRequest.Builder(SyncWorker.class).build());

        // Pequeño retardo de debounce visual para evitar clics dobles innecesarios
        btnSincronizarParcial.postDelayed(() -> {
            if (isAdded()) {
                btnSincronizarParcial.setEnabled(true);
                Toast.makeText(getContext(), "✓ Cola de sincronización incremental activada.", Toast.LENGTH_SHORT).show();
                cargarHistorialYCalcularCuadre();
            }
        }, 2000);
    }

    private void abrirDialogoAnulacionDinamico(Boleto boletoAfectado) {
        if (boletoAfectado.anulado) {
            Toast.makeText(getContext(), "Este pasaje ya fue anulado previamente.", Toast.LENGTH_SHORT).show();
            return;
        }

        long tiempoActual = System.currentTimeMillis();
        long tiempoEmision = boletoAfectado.fechaHora;
        long diferenciaMilisegundos = tiempoActual - tiempoEmision;

        if (diferenciaMilisegundos > 60000) {
            new AlertDialog.Builder(getContext())
                    .setTitle("ACCIÓN BLOQUEADA")
                    .setMessage("El tiempo límite de gracia para la auto-anulación ha expirado (Máximo 60 segundos desde su emisión).\n\nCualquier corrección posterior debe ser reportada directamente al área de liquidación en la central.")
                    .setPositiveButton("Entendido", null)
                    .show();
            return;
        }

        new AlertDialog.Builder(getContext())
                .setTitle("ADVERTENCIA DE AUDITORÍA FÍSICA")
                .setMessage("Al dar de baja este boleto por error de tipeo, el sistema generará de manera mandatoria un registro de incidencia transaccional.\n\n" +
                        " REGLA DE NEGOCIO: Está obligado a retener, tachar y entregar este ticket impreso físicamente al encargado del paradero de Lima durante la liquidación de su ruta.\n\n" +
                        "PENALIZACIÓN: Si al finalizar el turno no presenta el papel físico correspondiente a este código, el valor del pasaje será DESCONTADO automáticamente de su pago por servicio.\n\n" +
                        "¿Está seguro de que desea proceder?")
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
            Toast.makeText(getContext(), "Boleto anulado localmente. Guarde el comprobante impreso.", Toast.LENGTH_LONG).show();
            cargarHistorialYCalcularCuadre();
        });
        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void liquidarJornadaFinal() {
        final Turno turno = repo.getTurnoActivo();
        if (turno == null) return;

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
        turno.activo = false;
        turno.cierre = System.currentTimeMillis();
        repo.cerrarTurno(turno);

        Toast.makeText(getContext(), " Turno liquidado. Sincronizando arqueo contable...", Toast.LENGTH_LONG).show();

        if (isAdded() && getActivity() != null) {
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new LoginFragment())
                    .commit();
        }
    }
}