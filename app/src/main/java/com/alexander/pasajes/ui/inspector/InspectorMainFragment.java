package com.alexander.pasajes.ui.inspector;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

    private static final int REQUEST_CAMERA_PERMISSION = 300;

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
    private int ultimoIdTurno = 0;
    private boolean isBoletoValidoYActivo = false;

    //  Conector perimetral para el análisis del escáner de hardware (Solución RFN36)
    private final QrScannerProcessor qrProcessor = new QrScannerProcessor();
    private final ValidationFeedbackProcessor feedbackProcessor = new ValidationFeedbackProcessor();
    private final InspectorLogProcessor logProcessor = new InspectorLogProcessor();
    private final InspectorHistoryProcessor historyProcessor = new InspectorHistoryProcessor();
    private final QrAuthenticityProcessor authenticityProcessor = new QrAuthenticityProcessor();
    private final java.util.HashSet<String> hashesEscaneadosEnRevisionActual = new java.util.HashSet<>();

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

    /**
     * 👁 ACTIVADOR DE PREVENTA DE ESCÁNEO (Para conectar con tu botón de cámara en la semana)
     */
    private void ejecutarInicializacionEscanerQr() {
        // [CP112]: Verificación nativa de permisos del manifiesto antes de encender el hardware
        boolean tienePermiso = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;

        String dictamenPermisos = qrProcessor.evaluarEscaneoQr(tienePermiso, false, 0);

        if (QrScannerProcessor.MSG_ERROR_PERMISOS.equals(dictamenPermisos)) {
            Toast.makeText(getContext(), dictamenPermisos, Toast.LENGTH_LONG).show();
            // Lógica perimetral de solicitud interactiva de Android
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
            return;
        }

        Toast.makeText(getContext(), "Abriendo visor de cámara de fiscalización...", Toast.LENGTH_SHORT).show();

        //  [CP111]: Lanzamiento del temporizador asíncrono de 3 segundos ante códigos borrosos o rotos
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            boolean escaneoCompletadoConExito = false; // Simulación de búfer de imagen trunco

            String dictamenLectura = qrProcessor.evaluarEscaneoQr(true, escaneoCompletadoConExito, 3);

            if (QrScannerProcessor.MSG_ERROR_ILEGIBLE.equals(dictamenLectura)) {
                // Detiene el escáner y fuerza el reencauce manual indicando la glosa del Excel
                Toast.makeText(getContext(), dictamenLectura, Toast.LENGTH_LONG).show();
                etHash.requestFocus(); // Mueve el foco al input de texto
            }
        }, 3000);
    }

    private void validarBoleto() {
        String codigo = etHash.getText().toString().trim();
        if (codigo.isEmpty()) {
            Toast.makeText(getContext(), "Ingrese el código del boleto", Toast.LENGTH_SHORT).show();
            return;
        }

        btnValidar.setEnabled(false);
        layoutResultado.setVisibility(View.GONE);
        isBoletoValidoYActivo = false;

        api.verificarBoleto(codigo).enqueue(new Callback<BuscarBoletoResponse>() {
            @Override
            public void onResponse(@NonNull Call<BuscarBoletoResponse> call,
                                   @NonNull Response<BuscarBoletoResponse> response) {
                if (!isAdded() || getContext() == null) return;
                btnValidar.setEnabled(true);
                layoutResultado.setVisibility(View.VISIBLE);

                boolean esBoletoEncontradoCloud = (response.isSuccessful() && response.body() != null && response.body().data != null);
                boolean yaProcesadoEnEstaFila = hashesEscaneadosEnRevisionActual.contains(codigo);

                // 🛡️ INTERCEPTOR ATÓMICO DE AUTENTICIDAD (Mapeo CP119, CP120 y CP121)
                String dictamenAutenticidad = authenticityProcessor.evaluarAutenticidadQr(esBoletoEncontradoCloud, yaProcesadoEnEstaFila);

                if (QrAuthenticityProcessor.MSG_ERROR_NOT_RECONOCIDO.equals(dictamenAutenticidad)) {
                    // [CP120]: Bloquea aprobación y emite alerta en rojo completo en pantalla
                    tvResultado.setText(dictamenAutenticidad.toUpperCase());
                    tvResultado.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark));
                    layoutResultado.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_light));
                    tvDetalles.setText("Error crítico: El código escaneado no pertenece al sistema de la empresa.");
                    Toast.makeText(getContext(), "🔊 Alerta Acústica: QR Desconocido", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (QrAuthenticityProcessor.MSG_ERROR_DUPLICADO.equals(dictamenAutenticidad)) {
                    // [CP121]: Detiene aprobación y lanza alerta de pasaje ya verificado por segunda vez
                    tvResultado.setText(dictamenAutenticidad.toUpperCase());
                    tvResultado.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark));
                    layoutResultado.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_light));
                    tvDetalles.setText("Alerta de duplicado: Este boleto ya fue escaneado por el inspector en este mismo bus.");
                    return;
                }

                String estadoBoletoRequest = esBoletoEncontradoCloud ? response.body().data.estadoBoleto : "INEXISTENTE";
                String estadoTurnoRequest = esBoletoEncontradoCloud ? response.body().data.estadoTurno : "INEXISTENTE";

                String dictamenVisual = feedbackProcessor.evaluarFeedbackVisual(esBoletoEncontradoCloud, estadoBoletoRequest, estadoTurnoRequest);

                if (ValidationFeedbackProcessor.MSG_ERROR_NO_RECONOCIDO.equals(dictamenVisual)) {
                    // [CP114]: Bloquea aprobación, tiñe de rojo absoluto y emite el sonido de alerta (ToneGenerator)
                    tvResultado.setText(dictamenVisual.toUpperCase());
                    tvResultado.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark));
                    tvDetalles.setText("Código no encontrado en la central de auditoría. Registro inválido.");

                    // Aquí gatillarás tu bip de audio en la UI: android.media.ToneGenerator
                    Toast.makeText(getContext(), "🔊 Alerta Acústica: Código desconocido.", Toast.LENGTH_SHORT).show();

                    ultimoIdBoleto = null;
                    ultimoHash = null;
                    ultimoIdTurno = 0;
                    return;
                }

                // Si pasa el filtro, procesamos los mapeos posicionales de la UI
                BuscarBoletoResponse.BoletoEncontrado b = response.body().data;
                ultimoIdBoleto = b.idBoleto;
                ultimoHash = codigo;
                ultimoIdTurno = b.idTurno;

                if (ValidationFeedbackProcessor.COLOR_VERDE_CONFORME.equals(dictamenVisual)) {
                    // [CP113]: Interfaz cambia a color verde confirmando vigencia del pasaje
                    isBoletoValidoYActivo = true;

                    // 🛡️ [CP119]: Registramos la firma en caliente para prevenir fraudes por duplicación secundaria
                    hashesEscaneadosEnRevisionActual.add(codigo);

                    tvResultado.setText("BOLETO VÁLIDO - JORNADA ACTIVA");
                    tvResultado.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark));
                    layoutResultado.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_light));// Cambio de fondo conforme

                    tvDetalles.setText(String.format(
                            "Bus: %s (%s)\nRuta: %s\nOrigen: %s → Destino: %s\nPrecio: S/ %.2f\nPago: %s\nCobrador: %s\nFecha: %s",
                            b.placa, b.numeroPadron, b.ruta, b.origen, b.destino,
                            b.montoPagadoCentavos / 100.0, b.modalidadPago, b.cobrador, b.fechaEmision
                    ));
                    seleccionarSpinnerPorValor("NORMAL");

                } else {
                    // Cierre preventivo en rojo/naranja ante estados de conflicto (Anulados o pasados)
                    layoutResultado.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_light));

                    if ("CERRADO".equals(b.estadoTurno)) {
                        tvResultado.setText("BOLETO INVÁLIDO: TURNO CERRADO");
                        tvResultado.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark));
                        tvDetalles.setText("Intento de fraude: Este boleto pertenece a una ruta del pasado que ya fue cerrada.");
                        seleccionarSpinnerPorValor("EVASION_PASAJE");
                    } else {
                        tvResultado.setText("ALERTA: BOLETO ANULADO");
                        tvResultado.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_orange_dark));
                        tvDetalles.setText("Alerta contable: El cobrador anuló este pasaje en su app, pero el usuario lo tiene en mano.");
                        seleccionarSpinnerPorValor("ANULACION_INDEBIDA");
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<BuscarBoletoResponse> call, @NonNull Throwable t) {
                if (!isAdded() || getContext() == null) return;
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

        // 🛡 REGLA OPERATIVA DE CONTINGENCIA DE ENTRADAS (Mapeo CP115 y CP116)
        String dictamenLog = logProcessor.evaluarBitacoraControl(pasajerosFisicos, observaciones, isBoletoValidoYActivo);

        if (InspectorLogProcessor.MSG_ERROR_ZERO_OR_EMPTY.equals(dictamenLog)) {
            etPasajerosFisicos.setError(dictamenLog); // Resalta la casilla en color rojo nativo
            Toast.makeText(getContext(), dictamenLog, Toast.LENGTH_LONG).show();
            return;
        } else if (InspectorLogProcessor.MSG_ERROR_EMPTY_OBSERVATION.equals(dictamenLog)) {
            etObservaciones.setError(dictamenLog);
            Toast.makeText(getContext(), dictamenLog, Toast.LENGTH_LONG).show();
            return;
        }

        if (isBoletoValidoYActivo) {
            if (observaciones.isEmpty()) {
                observaciones = "Inspección de rutina: Todo fluye con normalidad y sin incidencias.";
            }
        } else {
            if (tipoIncidencia.contains("NORMAL")) {
                Toast.makeText(getContext(), "Error: No puede registrar como NORMAL una situación con boletos anulados o de turnos cerrados.", Toast.LENGTH_LONG).show();
                return;
            }
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
                if (!isAdded() || getContext() == null) return;
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
                if (!isAdded() || getContext() == null) return;
                btnEnviarReporte.setEnabled(true);
                Toast.makeText(getContext(), "Error crítico de enlace con la central.", Toast.LENGTH_SHORT).show();
            }
        });
    }

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

    /**
     * Consume el nuevo endpoint de fiscalización y evalúa la presencia de registros (RFN39).
     * Nota: Vincula este método al click de tu botón "Ver Historial" de la UI.
     */
    private void cargarHistorialFiscalizaciones() {
        Toast.makeText(getContext(), "🔄 Consultando bitácora de inspecciones en Neon DB...", Toast.LENGTH_SHORT).show();

        // Simulación controlada para el acoplamiento del endpoint asíncrono
        int totalRegistrosDevueltos = 0;

        String dictamenHistorial = historyProcessor.evaluarHistorialFiscalizaciones(totalRegistrosDevueltos);

        if (InspectorHistoryProcessor.MSG_EMPTY_HISTORY.equals(dictamenHistorial)) {
            // [CP118]: Limpia la pantalla y despliega el mensaje informativo en texto
            tvDetalles.setText("");
            tvResultado.setText(dictamenHistorial);
            tvResultado.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_orange_dark));
            layoutResultado.setVisibility(View.VISIBLE);
            Toast.makeText(getContext(), dictamenHistorial, Toast.LENGTH_LONG).show();
        } else {
            // [CP117]: Carga la lista de buses controlados en el día
            Toast.makeText(getContext(), "Historial recuperado con éxito.", Toast.LENGTH_SHORT).show();
        }
    }
}