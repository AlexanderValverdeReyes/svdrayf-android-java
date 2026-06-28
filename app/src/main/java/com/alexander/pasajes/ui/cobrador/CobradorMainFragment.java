package com.alexander.pasajes.ui.cobrador;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.alexander.pasajes.MainActivity;
import com.alexander.pasajes.R;
import com.alexander.pasajes.data.entity.Bus;
import com.alexander.pasajes.data.entity.Ruta;
import com.alexander.pasajes.data.entity.Turno;
import com.alexander.pasajes.network.ApiService;
import com.alexander.pasajes.network.RetrofitClient;
import com.alexander.pasajes.network.model.MaestrosResponse;
import com.alexander.pasajes.network.model.TurnoAperturaResponse;
import com.alexander.pasajes.network.model.TurnoAperturaRequest;
import com.alexander.pasajes.repository.AppRepository;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.ArrayList;
import java.util.List;

public class CobradorMainFragment extends Fragment {

    private Spinner spinnerBus, spinnerRuta;
    private Spinner spinnerTipoDia;
    private Button btnAbrirTurno;
    private ProgressBar progressBar;
    private AppRepository repo;
    private List<Bus> listaBuses = new ArrayList<>();
    private List<Ruta> listaRutas = new ArrayList<>();
    private ArrayAdapter<Bus> busAdapter;
    private ArrayAdapter<Ruta> rutaAdapter;
    private int idUsuario;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_cobrador_main, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repo = new AppRepository(requireContext());

        SharedPreferences prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        idUsuario = prefs.getInt("id_usuario", 0);

        spinnerTipoDia = view.findViewById(R.id.spinnerTipoDia);
        spinnerBus = view.findViewById(R.id.spinnerBus);
        spinnerRuta = view.findViewById(R.id.spinnerRuta);
        btnAbrirTurno = view.findViewById(R.id.btnAbrirTurno);
        progressBar = view.findViewById(R.id.progressBar);

        descargarMaestros();
    }

    private void descargarMaestros() {
        progressBar.setVisibility(View.VISIBLE);
        ApiService api = RetrofitClient.getApiService(requireContext());
        api.getMaestros().enqueue(new Callback<MaestrosResponse>() {
            @Override
            public void onResponse(@NonNull Call<MaestrosResponse> call, @NonNull Response<MaestrosResponse> response) {
                // 🛡️ ESCUDO DE ASINCRONÍA CONSOLE LOG: Evita que la app muera si el operador cambia de pantalla
                if (!isAdded() || getContext() == null) return;

                if (response.isSuccessful() && response.body() != null) {
                    repo.guardarMaestros(response.body());
                }
                cargarSpinners();
            }

            @Override
            public void onFailure(@NonNull Call<MaestrosResponse> call, @NonNull Throwable t) {
                if (!isAdded() || getContext() == null) return;
                cargarSpinners();
            }
        });
    }

    private void cargarSpinners() {
        if (!isAdded() || getContext() == null) return;

        listaBuses = repo.getAllBuses();
        listaRutas = repo.getRutas();

        String[] opcionesDia = {"Día de Semana (Normal)", "Domingo / Feriado"};
        ArrayAdapter<String> diaAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, opcionesDia);
        diaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTipoDia.setAdapter(diaAdapter);

        busAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, listaBuses);
        busAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBus.setAdapter(busAdapter);

        rutaAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, listaRutas);
        rutaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRuta.setAdapter(rutaAdapter);

        progressBar.setVisibility(View.GONE);

        btnAbrirTurno.setOnClickListener(v -> {
            Bus busSeleccionado = (Bus) spinnerBus.getSelectedItem();
            Ruta rutaSeleccionada = (Ruta) spinnerRuta.getSelectedItem();

            int seleccionDia = spinnerTipoDia.getSelectedItemPosition();
            String tipoDiaStr = (seleccionDia == 0) ? "NORMAL" : "FERIADO";

            if (busSeleccionado != null && rutaSeleccionada != null) {
                SharedPreferences prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
                prefs.edit().putString("regimen_dia_activo", tipoDiaStr).apply();

                abrirTurno(busSeleccionado.id, rutaSeleccionada.id, rutaSeleccionada.tipo);
            }
        });
    }

    private void abrirTurno(int busId, int rutaId, String tipoRuta) {
        btnAbrirTurno.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        final Turno turnoLocal = new Turno();
        turnoLocal.vendedorId = idUsuario;
        turnoLocal.busId = busId;
        turnoLocal.rutaId = rutaId;
        turnoLocal.apertura = System.currentTimeMillis();
        turnoLocal.activo = true;

        ApiService api = RetrofitClient.getApiService(requireContext());
        api.abrirTurno(new TurnoAperturaRequest(busId, rutaId)).enqueue(new Callback<TurnoAperturaResponse>() {
            @Override
            public void onResponse(@NonNull Call<TurnoAperturaResponse> call, @NonNull Response<TurnoAperturaResponse> response) {
                if (!isAdded() || getContext() == null) return;
                progressBar.setVisibility(View.GONE);
                btnAbrirTurno.setEnabled(true);

                if (response.isSuccessful() && response.body() != null && response.body().turno != null) {
                    // 🚀 ENLACE EXITOSO: Guardamos el ID serial real de PostgreSQL en Room local
                    turnoLocal.serverTurnoId = response.body().turno.idTurno;
                    turnoLocal.sincronizado = true;

                    long idGenerado = repo.abrirTurno(turnoLocal);
                    completarTransicionVenta(idGenerado, tipoRuta, rutaId, busId);
                } else if (response.code() == 409) {
                    Toast.makeText(getContext(), "⚠️ Conflicto: Esta unidad o su cuenta ya registran jornada activa.", Toast.LENGTH_LONG).show();
                } else {
                    abrirTurnoContingenciaOffline(turnoLocal, tipoRuta, rutaId, busId);
                }
            }

            @Override
            public void onFailure(@NonNull Call<TurnoAperturaResponse> call, @NonNull Throwable t) {
                if (!isAdded() || getContext() == null) return;
                abrirTurnoContingenciaOffline(turnoLocal, tipoRuta, rutaId, busId);
            }
        });
    }

    private void abrirTurnoContingenciaOffline(Turno turnoLocal, String tipoRuta, int rutaId, int busId) {
        progressBar.setVisibility(View.GONE);
        btnAbrirTurno.setEnabled(true);
        Toast.makeText(getContext(), "📶 Modo Contingencia: Registrando en Room Local.", Toast.LENGTH_SHORT).show();
        turnoLocal.serverTurnoId = 0; // Se enlazará al sincronizar
        turnoLocal.sincronizado = false;
        long idGenerado = repo.abrirTurno(turnoLocal);
        completarTransicionVenta(idGenerado, tipoRuta, rutaId, busId);
    }

    private void completarTransicionVenta(long turnoLocalId, String tipoRuta, int rutaId, int busId) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).irAVenta(turnoLocalId, tipoRuta, rutaId, busId);
        }
    }
}