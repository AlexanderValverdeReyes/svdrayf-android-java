package com.alexander.pasajes.ui.inspector;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.alexander.pasajes.R;
import com.alexander.pasajes.network.ApiService;
import com.alexander.pasajes.network.RetrofitClient;
import com.alexander.pasajes.network.model.*;
import com.alexander.pasajes.repository.AppRepository;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class InspectorMainFragment extends Fragment {

    private EditText etHash;
    private Button btnValidar;
    private LinearLayout layoutResultado;
    private TextView tvResultado, tvDetalles;
    private EditText etPasajerosFisicos, etObservaciones;
    private Spinner spinnerTipoIncidencia;
    private Button btnEnviarReporte;
    private ApiService api;
    private AppRepository repo;

    private String ultimoIdBoleto = null;
    private String ultimoHash = null;

    // ADICIÓN CRÍTICA: Variable global para retener el id_turno legítimo enviado por Neon DB
    private int ultimoIdTurno = 0;

    // ADICIÓN DE CONTROL: Determina si el boleto es vigente para habilitar la opción NORMAL
    private boolean isBoletoValidoYActivo = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_inspector_main, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        api = RetrofitClient.getApiService(requireContext());
        repo = new AppRepository(requireContext());

        etHash = view.findViewById(R.id.etHash);
        btnValidar = view.findViewById(R.id.btnValidar);
        layoutResultado = view.findViewById(R.id.layoutResultado);
        tvResultado = view.findViewById(R.id.tvResultado);
        tvDetalles = view.findViewById(R.id.tvDetalles);
        etPasajerosFisicos = view.findViewById(R.id.etPasajerosFisicos);
        etObservaciones = view.findViewById(R.id.etObservaciones);
        spinnerTipoIncidencia = view.findViewById(R.id.spinnerTipoIncidencia);
        btnEnviarReporte = view.findViewById(R.id.btnEnviarReporte);

        btnValidar.setOnClickListener(v -> validarBoleto());
        btnEnviarReporte.setOnClickListener(v -> enviarReporte());
    }

    private void validarBoleto() {
        String codigo = etHash.getText().toString().trim();
        if (codigo.isEmpty()) {
            Toast.makeText(getContext(), "Ingrese el código del boleto", Toast.LENGTH_SHORT).show();
            return;
        }

        btnValidar.setEnabled(false);
        layoutResultado.setVisibility(View.GONE);
        isBoletoValidoYActivo = false; // Resetear bandera de control en cada consulta

        api.verificarBoleto(codigo).enqueue(new Callback<BuscarBoletoResponse>() {
            @Override
            public void onResponse(@NonNull Call<BuscarBoletoResponse> call,
                                   @NonNull Response<BuscarBoletoResponse> response) {
                btnValidar.setEnabled(true);
                layoutResultado.setVisibility(View.VISIBLE);

                if (response.isSuccessful() && response.body() != null && response.body().data != null) {
                    BuscarBoletoResponse.BoletoEncontrado b = response.body().data;
                    ultimoIdBoleto = b.idBoleto;
                    ultimoHash = codigo;
                    ultimoIdTurno = b.idTurno;

                    // ESCENARIO A: El turno ya fue CERRADO (Fechas pasadas o liquidación culminada)
                    if ("CERRADO".equals(b.estadoTurno)) {
                        tvResultado.setText("BOLETO INVÁLIDO: TURNO CERRADO");
                        tvResultado.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark));
                        tvDetalles.setText("Intento de fraude: Este boleto pertenece a una ruta del pasado que ya fue cerrada.");
                        seleccionarSpinnerPorValor("EVASION_PASAJE");

                        // ESCENARIO B: El boleto figura como ANULADO en el sistema pero se encuentra físico
                    } else if ("ANULADO".equals(b.estadoBoleto)) {
                        tvResultado.setText("ALERTA: BOLETO ANULADO");
                        tvResultado.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_orange_dark));
                        tvDetalles.setText("Alerta contable: El cobrador anuló este pasaje en su app, pero el usuario lo tiene en mano.");
                        seleccionarSpinnerPorValor("ANULACION_INDEBIDA");

                        // ESCENARIO C: El boleto es VÁLIDO y pertenece a un viaje en ejecución (ABIERTO)
                    } else if ("VALIDO".equals(b.estadoBoleto) && "ABIERTO".equals(b.estadoTurno)) {
                        isBoletoValidoYActivo = true; // Activa permiso para reporte NORMAL
                        tvResultado.setText("BOLETO VÁLIDO - JORNADA ACTIVA");
                        tvResultado.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark));

                        tvDetalles.setText(String.format(
                                "Bus: %s (%s)\nRuta: %s\nOrigen: %s → Destino: %s\nPrecio: S/ %.2f\nPago: %s\nCobrador: %s\nFecha: %s",
                                b.placa, b.numeroPadron, b.ruta, b.origen, b.destino,
                                b.montoPagadoCentavos / 100.0, b.modalidadPago,
                                b.cobrador, b.fechaEmision
                        ));
                        seleccionarSpinnerPorValor("NORMAL");
                    }
                } else {
                    tvResultado.setText("BOLETO INEXISTENTE");
                    tvResultado.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark));
                    tvDetalles.setText("Código no encontrado en la central de auditoría.");
                    ultimoIdBoleto = null;
                    ultimoHash = null;
                    ultimoIdTurno = 0;
                }
            }

            @Override
            public void onFailure(@NonNull Call<BuscarBoletoResponse> call, @NonNull Throwable t) {
                btnValidar.setEnabled(true);
                Toast.makeText(getContext(), "Error de conexión al validar", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void enviarReporte() {
        if (ultimoIdTurno <= 0) {
            Toast.makeText(getContext(), "Primero debe validar un boleto mediante Hash para asociar el Turno actual.", Toast.LENGTH_LONG).show();
            return;
        }

        String pasajerosStr = etPasajerosFisicos.getText().toString().trim();
        if (pasajerosStr.isEmpty()) {
            Toast.makeText(getContext(), "Ingrese la cantidad de pasajeros", Toast.LENGTH_SHORT).show();
            return;
        }

        int pasajerosFisicos;
        try {
            pasajerosFisicos = Integer.parseInt(pasajerosStr);
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Número de pasajeros no válido", Toast.LENGTH_SHORT).show();
            return;
        }

        String observaciones = etObservaciones.getText().toString().trim();
        String tipoIncidencia = spinnerTipoIncidencia.getSelectedItem() != null ?
                spinnerTipoIncidencia.getSelectedItem().toString() : "NORMAL";

        //  REGLAS DE NEGOCIO PARA CONSOLIDACIÓN DE REPORTES
        if (isBoletoValidoYActivo) {
            // Si la operación es correcta, permitimos enviar como NORMAL con texto automático si está vacío
            if (observaciones.isEmpty()) {
                observaciones = "Inspección de rutina: Todo fluye con normalidad y sin incidencias.";
            }
        } else {
            // Bloqueo preventivo: Si el boleto es inválido/anulado/pasado, no se puede marcar como NORMAL
            if (tipoIncidencia.contains("NORMAL")) {
                Toast.makeText(getContext(), "Error: No puede registrar como NORMAL una situación con boletos anulados o de turnos cerrados.", Toast.LENGTH_LONG).show();
                return;
            }
            // Forzar descripción de auditoría obligatoria en caso de irregularidades
            if (observaciones.isEmpty()) {
                Toast.makeText(getContext(), "Debe ingresar una observación manual detallando la irregularidad encontrada.", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        String descripcionFinal = observaciones + " (código: " + ultimoHash + ")";

        IncidenciaRequest request = new IncidenciaRequest();
        request.tipo_incidencia = tipoIncidencia;
        request.descripcion = descripcionFinal;
        request.pasajeros_fisicos = pasajerosFisicos;
        request.id_boleto_afectado = ultimoIdBoleto;
        request.id_turno = ultimoIdTurno;

        btnEnviarReporte.setEnabled(false);
        api.inspeccion(request).enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(@NonNull Call<GenericResponse> call, @NonNull Response<GenericResponse> response) {
                btnEnviarReporte.setEnabled(true);
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "Reporte de fiscalización enviado correctamente.", Toast.LENGTH_SHORT).show();
                    etPasajerosFisicos.setText("");
                    etObservaciones.setText("");
                    layoutResultado.setVisibility(View.GONE);
                    ultimoIdBoleto = null;
                    ultimoHash = null;
                    ultimoIdTurno = 0;
                    isBoletoValidoYActivo = false;
                } else {
                    Toast.makeText(getContext(), "Error del servidor al enviar reporte (Código " + response.code() + ").", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<GenericResponse> call, @NonNull Throwable t) {
                btnEnviarReporte.setEnabled(true);
                Toast.makeText(getContext(), "Error crítico de enlace con la central.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 🚀 MÉTODO AUXILIAR AUTOMÁTICO: Busca e iguala la selección del spinner según las reglas lógicas
    private void seleccionarSpinnerPorValor(String valorBuscar) {
        SpinnerAdapter adapter = spinnerTipoIncidencia.getAdapter();
        if (adapter == null) return;
        for (int i = 0; i < adapter.getCount(); i++) {
            String itemStr = adapter.getItem(i).toString().toUpperCase();
            if (itemStr.contains(valorBuscar.toUpperCase()) || valorBuscar.toUpperCase().contains(itemStr)) {
                spinnerTipoIncidencia.setSelection(i);
                break;
            }
        }
    }
}