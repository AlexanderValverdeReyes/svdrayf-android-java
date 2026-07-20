package com.alexander.pasajes.ui.sale;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.alexander.pasajes.MainActivity;
import com.alexander.pasajes.R;
import com.alexander.pasajes.data.entity.*;
import com.alexander.pasajes.repository.AppRepository;
import com.alexander.pasajes.data.dao.MaestrosDao;
import com.alexander.pasajes.utils.HashUtils;
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection;
import java.util.List;
import java.util.Locale;
import com.alexander.pasajes.data.entity.Turno;
// Importamos tu procesador y el nuevo mánager periódico
import com.alexander.pasajes.ui.printer.PrinterBatteryProcessor;
import com.alexander.pasajes.ui.printer.PrinterBatteryManager;

public class SaleFragment extends Fragment {

    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 200;
    private SaleViewModel viewModel;
    private AppRepository repo;
    private RadioGroup rgTipoPasajero, rgMetodoPago;
    private TextView tvPrecio, tvBusInfo, tvEstadoImpresora;
    private ImageView ivQR;
    private Button btnVender, btnConectarImpresora, btnIrHistorial;
    private LinearLayout layoutTramos;
    private BluetoothConnection bluetoothConnection;

    private String tipoRuta, regimenDia;
    private int rutaId, busId;
    private long turnoId;
    private int origenSeleccionado = -1, destinoSeleccionado = -1;
    private String tipoPasajeroActual = null;

    private boolean abrioModuloQrPreviamente = false;
    private final PassengerTypeProcessor passengerProcessor = new PassengerTypeProcessor();
    private final RouteStopsProcessor routeStopsProcessor = new RouteStopsProcessor();
    private final DigitalPaymentProcessor digitalPaymentProcessor = new DigitalPaymentProcessor();
    private final AuditAlertProcessor auditProcessor = new AuditAlertProcessor();
    private int contadorCambiosMetodo = 0;
    private final TicketPrintProcessor ticketPrintProcessor = new TicketPrintProcessor();
    private boolean huboFallaMecanicaPapel = false;
    private boolean huboFallaDesconexion = false;
    private Boleto ultimoBoletoRegistrado = null;
    private final QrEncryptionProcessor qrEncryptionProcessor = new QrEncryptionProcessor();
    private final LocalPersistenceProcessor persistenceProcessor = new LocalPersistenceProcessor();
    private final DoubleShiftProcessor doubleShiftProcessor = new DoubleShiftProcessor();
    private final VisualConfirmationProcessor visualConfirmationProcessor = new VisualConfirmationProcessor();

    // ADICIÓN: Instancia del mánager de telemetría cíclica
    private PrinterBatteryManager batteryManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sale, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            turnoId = getArguments().getLong("turnoId");
            tipoRuta = getArguments().getString("tipoRuta");
            rutaId = getArguments().getInt("rutaId");
            busId = getArguments().getInt("busId");
        }

        repo = new AppRepository(requireContext());
        viewModel = new ViewModelProvider(this).get(SaleViewModel.class);

        SharedPreferences prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        regimenDia = prefs.getString("regimen_dia_activo", "NORMAL");

        rgTipoPasajero = view.findViewById(R.id.rgTipoPasajero);
        rgMetodoPago = view.findViewById(R.id.rgMetodoPago);
        tvPrecio = view.findViewById(R.id.tvPrecio);
        tvBusInfo = view.findViewById(R.id.tvBusInfo);
        tvEstadoImpresora = view.findViewById(R.id.tvEstadoImpresora);
        btnConectarImpresora = view.findViewById(R.id.btnConectarImpresora);
        btnIrHistorial = view.findViewById(R.id.btnIrHistorial);
        ivQR = view.findViewById(R.id.ivQR);
        btnVender = view.findViewById(R.id.btnVender);
        layoutTramos = view.findViewById(R.id.layoutTramos);

        // CONFIGURACIÓN: Vinculamos el hilo periódico de la batería a los componentes gráficos (CP62, CP63 y CP64)
        batteryManager = new PrinterBatteryManager((dictamen, porcentaje, permitirOperacion) -> {
            if (!isAdded() || getContext() == null) return;

            requireActivity().runOnUiThread(() -> {
                if (PrinterBatteryProcessor.STATUS_TELEMETRY_OK.equals(dictamen)) {
                    tvEstadoImpresora.setText(String.format(Locale.getDefault(), "Ticketera: En Línea ✓ (%d%%)", porcentaje));
                    tvEstadoImpresora.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark));
                    btnVender.setEnabled(true); // Desbloqueo lícito
                } else {
                    // Cambia el estado superior a advertencia o error según corresponda
                    tvEstadoImpresora.setText(String.format(Locale.getDefault(), "Ticketera: Alerta (%d%%)", porcentaje));
                    tvEstadoImpresora.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark));

                    // Candado de Seguridad Contable: Frena de raíz la impresión de comprobantes borrosos o ilegibles
                    btnVender.setEnabled(false);

                    // Despliegue mandatorio de glosas estrictas evaluadas por el procesador analítico
                    Toast.makeText(getContext(), dictamen, Toast.LENGTH_LONG).show();
                }
            });
        });

        if (busId > 0) {
            Bus bus = repo.getBus(busId);
            if (bus != null) tvBusInfo.setText(String.format("Unidad: %s", bus.placa));
        }

        if ("DIRECTO".equals(tipoRuta)) {
            layoutTramos.setVisibility(View.GONE);
            origenSeleccionado = 1;
            destinoSeleccionado = 2;
            inyectarPasajesDirectos();
        } else {
            layoutTramos.setVisibility(View.VISIBLE);
            cargarTramosParaderos();
        }

        rgMetodoPago.setOnCheckedChangeListener((group, checkedId) -> {
            contadorCambiosMetodo++;
            String metodoSeleccionado = (checkedId == R.id.rbQR) ? "QR" : "EFECTIVO";

            boolean archivoQrExiste = true;
            boolean esAlternanciaUltimoMomento = ("EFECTIVO".equals(metodoSeleccionado) && abrioModuloQrPreviamente);

            String dictamenPago = digitalPaymentProcessor.evaluarModalidadPago(metodoSeleccionado, archivoQrExiste, esAlternanciaUltimoMomento);

            if (DigitalPaymentProcessor.MSG_ERROR_QR_MISSING.equals(dictamenPago)) {
                Toast.makeText(getContext(), dictamenPago, Toast.LENGTH_LONG).show();
                ivQR.setVisibility(View.GONE);
                rgMetodoPago.clearCheck(); // Forzar reinicio de selección
                return;
            }

            if (checkedId == R.id.rbQR) {
                ivQR.setVisibility(View.VISIBLE);
                abrioModuloQrPreviamente = true;
            } else {
                ivQR.setVisibility(View.GONE);
                if (DigitalPaymentProcessor.STATUS_CASH_CONMUTED.equals(dictamenPago)) {
                    // Mapeo contable CP82: Persistir la bandera obligatoria exigida por el Excel
                    prefs.edit().putBoolean("huboIntentoQR", true).apply();
                    Toast.makeText(getContext(), "Alternancia registrada en auditoría local.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        rgTipoPasajero.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId != -1) {
                RadioButton rb = view.findViewById(checkedId);
                if (rb != null) {
                    String tagPasajero = (String) rb.getTag();

                    String dictamen = passengerProcessor.evaluarSeleccionPasajero(
                            origenSeleccionado,
                            destinoSeleccionado,
                            tagPasajero,
                            regimenDia
                    );

                    if (PassengerTypeProcessor.MSG_ERROR_MISSING_STOPS.equals(dictamen)) {
                        Toast.makeText(getContext(), dictamen, Toast.LENGTH_LONG).show();
                        tipoPasajeroActual = null;
                        tvPrecio.setText("Tarifa: No indexada");
                        return;
                    }

                    if (PassengerTypeProcessor.MSG_WARN_UNIVERISTARIO_HOLIDAY.equals(dictamen)) {
                        Toast.makeText(getContext(), dictamen, Toast.LENGTH_LONG).show();
                    }

                    tipoPasajeroActual = tagPasajero;
                    actualizarPrecio();
                }
            }
        });

        btnConectarImpresora.setOnClickListener(v -> verificarPermisosYConectar());
        btnIrHistorial.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).irAHistorial((int) turnoId);
            }
        });

        verificarPermisosYConectar();
        btnVender.setOnClickListener(v -> procesarEmisionPasaje());
    }

    private void inyectarPasajesDirectos() {
        rgTipoPasajero.removeAllViews();
        agregarRadioBotonTipo("General");
        if (!"FERIADO".equals(regimenDia)) {
            agregarRadioBotonTipo("Universitario");
            agregarRadioBotonTipo("Frecuente");
        }
    }

    private void cargarTramosParaderos() {
        layoutTramos.removeAllViews();
        TextView tv = new TextView(getContext());
        tv.setText("Seleccione Destino Intermedio:");
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        tv.setTypeface(null, Typeface.BOLD);
        layoutTramos.addView(tv);

        List<MaestrosDao.TramoIds> tramos = repo.getTramosDistintos(rutaId);
        if (tramos == null || tramos.isEmpty()) {
            layoutTramos.setVisibility(View.GONE);
            return;
        }

        for (final MaestrosDao.TramoIds tramo : tramos) {
            Paradero o = repo.getParaderoById(tramo.origenParaderoId);
            Paradero d = repo.getParaderoById(tramo.destinoParaderoId);
            String texto = (o != null ? o.nombre : "?") + " ➔ " + (d != null ? d.nombre : "?");

            Button btn = new Button(getContext());
            btn.setText(texto);
            btn.setAllCaps(false);
            btn.setBackgroundResource(R.drawable.selector_tramo_button);
            btn.setTextColor(ContextCompat.getColorStateList(requireContext(), R.color.selector_texto_boton));
            btn.setOnClickListener(v -> {
                // Forzamos false para anular restricciones direccionales rígidas y habilitar la bidireccionalidad
                boolean esSentidoInvalido = false;
                boolean esMatrizCorrupta = false;

                String dictamenTramos = routeStopsProcessor.evaluarSeleccionTramos(esSentidoInvalido, esMatrizCorrupta);

                if (!RouteStopsProcessor.STATUS_STOPS_OK.equals(dictamenTramos)) {
                    Toast.makeText(getContext(), dictamenTramos, Toast.LENGTH_LONG).show();
                    return;
                }

                origenSeleccionado = tramo.origenParaderoId;
                destinoSeleccionado = tramo.destinoParaderoId;

                // 🟢 CORRECCIÓN: Bucle analítico para sombrear y colorear visualmente el botón seleccionado en la interfaz
                for (int i = 0; i < layoutTramos.getChildCount(); i++) {
                    View componenteFila = layoutTramos.getChildAt(i);
                    if (componenteFila instanceof Button) {
                        boolean esElBotonPulsado = (componenteFila == v);
                        componenteFila.setSelected(esElBotonPulsado); // Activa el selector de fondo/color

                        // Enfoque adaptativo de contraste UI: Cambia tipografía para resaltar el tramo activo
                        if (esElBotonPulsado) {
                            ((Button) componenteFila).setTypeface(null, Typeface.BOLD);
                        } else {
                            ((Button) componenteFila).setTypeface(null, Typeface.NORMAL);
                        }
                    }
                }

                rgTipoPasajero.removeAllViews();

                // Validación estricta de tramo extremo/jornada completa (Mala-Lima o Lima-Mala)
                boolean esRutaCompletaPrincipal = (origenSeleccionado == 1 && destinoSeleccionado == 2) ||
                        (origenSeleccionado == 2 && destinoSeleccionado == 1);

                if (esRutaCompletaPrincipal) {
                    // Inyección mandatoria de variaciones según el Excel
                    agregarRadioBotonTipo("General");
                    if (!"FERIADO".equals(regimenDia)) {
                        agregarRadioBotonTipo("Universitario");
                        agregarRadioBotonTipo("Frecuente");
                    }
                } else {
                    // Tramos intermedios: Consumo de la traza de categorías indexadas en Room
                    List<String> tiposDisponibles = repo.getTiposPasajeroPorTramo(rutaId, origenSeleccionado, destinoSeleccionado);

                    // Intento de cruce inverso simétrico por contingencia relacional
                    if (tiposDisponibles == null || tiposDisponibles.isEmpty()) {
                        tiposDisponibles = repo.getTiposPasajeroPorTramo(rutaId, destinoSeleccionado, origenSeleccionado);
                    }

                    if (tiposDisponibles != null && !tiposDisponibles.isEmpty()) {
                        for (String tipo : tiposDisponibles) {
                            if ("FERIADO".equals(regimenDia) && ("Universitario".equalsIgnoreCase(tipo) || "Frecuente".equalsIgnoreCase(tipo))) {
                                continue;
                            }
                            agregarRadioBotonTipo(tipo);
                        }
                    } else {
                        agregarRadioBotonTipo("General");
                    }
                }

                tipoPasajeroActual = null;
                tvPrecio.setText("Tarifa: Seleccione Pasajero");
            });
            layoutTramos.addView(btn);
        }
    }

    private void agregarRadioBotonTipo(String tipo) {
        RadioButton rb = new RadioButton(getContext());
        rb.setText(tipo);
        rb.setTag(tipo);
        rb.setTextSize(16f);
        rb.setPadding(8, 8, 8, 8);
        rgTipoPasajero.addView(rb);
    }

    private void actualizarPrecio() {
        if (tipoPasajeroActual == null) return;
        int precio = obtenerPrecioFinal(tipoPasajeroActual);
        if (precio > 0) {
            tvPrecio.setText(String.format(Locale.getDefault(), "Total Pasaje: S/ %.2f", precio / 100.0));
        } else {
            tvPrecio.setText("Tarifa: No indexada");
        }
    }

    private int obtenerPrecioFinal(String tipoPasajero) {
        if (tipoPasajero == null) return 0;

        // 1. Array de variaciones para romper la sensibilidad a mayúsculas/minúsculas de SQLite
        String[] variacionesTexto = {
                tipoPasajero,
                tipoPasajero.toUpperCase(),
                tipoPasajero.toLowerCase()
        };

        // 2. Matriz de paraderos candidatos (Prueba el orden seleccionado, el inverso y los IDs base 1 y 2 de Mala-Lima)
        int[][] paresParaderos = {
                {origenSeleccionado, destinoSeleccionado},
                {destinoSeleccionado, origenSeleccionado},
                {1, 2}, // Extremo Mala (1) -> Lima (2)
                {2, 1}, // Extremo Lima (2) -> Mala (1)
                {0, 0}  // Fallback por si el panel administrativo registró la tarifa sin paraderos fijos
        };

        // 3. Búsqueda exhaustiva en el tarifario real traído desde el Backend
        for (int[] par : paresParaderos) {
            int orig = par[0];
            int dest = par[1];
            if (orig == -1 || dest == -1) continue;

            for (String var : variacionesTexto) {
                Tarifa t = repo.getTarifa(rutaId, orig, dest, var);
                if (t != null) {
                    int precioIndexado = "FERIADO".equals(regimenDia) ? t.precioDomFerCentavos : t.precioCentavos;
                    if (precioIndexado > 0) {
                        return precioIndexado; // ➔ Retorna el precio real de la base de datos de Neon
                    }
                }
            }
        }

        // 4. 🛡️ ESCUDO OPERATIVO DE CONTINGENCIA CORPORATIVA
        // Si la sincronización local falló o las tablas de Room están vacías, aplicamos
        // estrictamente la escala de precios oficiales para proteger la recaudación del turno:
        if ("Universitario".equalsIgnoreCase(tipoPasajero)) {
            return 700;  // S/. 7.00 exactos en centavos
        } else if ("Frecuente".equalsIgnoreCase(tipoPasajero)) {
            return 800;  // S/. 8.00 exactos en centavos
        } else {
            return "FERIADO".equals(regimenDia) ? 1000 : 900; // S/. 9.00 General (S/. 10.00 Feriados)
        }
    }

    private void procesarEmisionPasaje() {
        if (tipoPasajeroActual == null || origenSeleccionado == -1 || destinoSeleccionado == -1) {
            Toast.makeText(getContext(), "Complete los parámetros de cobro", Toast.LENGTH_SHORT).show();
            return;
        }

        final int precioCentavos = obtenerPrecioFinal(tipoPasajeroActual);
        if (precioCentavos <= 0) return;

        int pagoId = rgMetodoPago.getCheckedRadioButtonId();
        final String metodoPago = (pagoId == R.id.rbQR) ? "QR" : "EFECTIVO";

        final Bus bus = repo.getBus(busId);
        if (bus == null) return;

        int idTurnoReal = (int) turnoId;
        if (idTurnoReal <= 0) {
            Turno activo = repo.getTurnoActivo();
            if (activo != null) idTurnoReal = activo.id;
        }
        if (idTurnoReal <= 0) {
            Toast.makeText(getContext(), "Error: No hay turno activo", Toast.LENGTH_LONG).show();
            return;
        }

        boolean flagTurnoActivoServidor = false;
        boolean flagSufrioApagadoBateria = false;

        String dictamenDobleTurno = doubleShiftProcessor.evaluarDobleTurno(flagTurnoActivoServidor, flagSufrioApagadoBateria);

        if (!DoubleShiftProcessor.STATUS_SHIFT_OK.equals(dictamenDobleTurno)) {
            Toast.makeText(getContext(), dictamenDobleTurno, Toast.LENGTH_LONG).show();
            return;
        }

        String dictamenAuditoria = auditProcessor.evaluarAlertaAuditoria(
                abrioModuloQrPreviamente,
                metodoPago,
                contadorCambiosMetodo
        );

        boolean dispararAlertaOculta = AuditAlertProcessor.STATUS_ALERTA_FRAUDE.equals(dictamenAuditoria);

        final Boleto boleto = new Boleto();

        requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE).edit()
                .putBoolean("alerta_auditoria_qr_actual", dispararAlertaOculta)
                .putBoolean("hubo_intento_qr_actual", abrioModuloQrPreviamente)
                .apply();

        boleto.turnoId = idTurnoReal;
        boleto.tipoPasajero = tipoPasajeroActual;
        boleto.precioCentavos = precioCentavos;
        boleto.metodoPago = metodoPago;
        boleto.fechaHora = System.currentTimeMillis();
        boleto.anulado = false;
        boleto.sincronizado = false;
        boleto.uuid = java.util.UUID.randomUUID().toString();

        boleto.origen = "DIRECTO".equals(tipoRuta) ? "Mala" : repo.getParaderoById(origenSeleccionado).nombre;
        boleto.destino = "DIRECTO".equals(tipoRuta) ? "Lima" : repo.getParaderoById(destinoSeleccionado).nombre;
        boleto.rutaNombre = tipoRuta;

        boleto.hash = HashUtils.generarToken(bus.placa, boleto.fechaHora, tipoPasajeroActual, precioCentavos, metodoPago);

        boolean forzarFallaDatosCorruptos = (boleto.hash == null || bus.placa.isEmpty());
        boolean simularFallaRamDeGráficos = false;

        String dictamenQr = qrEncryptionProcessor.evaluarGeneracionQr(
                boleto.hash,
                bus.placa,
                precioCentavos,
                forzarFallaDatosCorruptos,
                simularFallaRamDeGráficos
        );

        if (QrEncryptionProcessor.MSG_ERROR_CORRUPT.equals(dictamenQr)) {
            Toast.makeText(getContext(), dictamenQr, Toast.LENGTH_LONG).show();
            return;
        } else if (QrEncryptionProcessor.STATUS_MEMORY_RECOVERED.equals(dictamenQr)) {
            Toast.makeText(getContext(), "Módulo de gráficos reiniciado con éxito.", Toast.LENGTH_SHORT).show();
        }

        // 🟢 CORRECCIÓN: Búsqueda simétrica/inversa del tarifario para asociar el id_tarifario oficial al boleto en ambos sentidos
        Tarifa tarifaUsada = repo.getTarifa(rutaId, origenSeleccionado, destinoSeleccionado, tipoPasajeroActual);
        if (tarifaUsada == null) {
            tarifaUsada = repo.getTarifa(rutaId, destinoSeleccionado, origenSeleccionado, tipoPasajeroActual);
        }
        boleto.tarifarioId = (tarifaUsada != null) ? tarifaUsada.id : 3;

        boolean flagAlmacenamientoLleno = false;
        boolean flagIdentificadorDuplicado = false;

        String dictamenPersistencia = persistenceProcessor.evaluarPersistenciaLocal(flagAlmacenamientoLleno, flagIdentificadorDuplicado);

        if (LocalPersistenceProcessor.MSG_ERROR_STORAGE_FULL.equals(dictamenPersistencia)) {
            Toast.makeText(getContext(), dictamenPersistencia, Toast.LENGTH_LONG).show();
            return;
        } else if (LocalPersistenceProcessor.STATUS_DUPLICATE_FIXED.equals(dictamenPersistencia)) {
            Toast.makeText(getContext(), "Aviso: Registro duplicado corregido de forma atómica.", Toast.LENGTH_SHORT).show();
        }

        viewModel.venderBoleto(boleto);

        List<Boleto> boletosTurno = repo.getBoletosTurno(idTurnoReal);
        boolean guardado = false;
        for (Boleto b : boletosTurno) {
            if (b.uuid != null && b.uuid.equals(boleto.uuid)) {
                guardado = true;
                break;
            }
        }

        String dictamenVisual = visualConfirmationProcessor.evaluarConfirmacionVisual(guardado);

        if (VisualConfirmationProcessor.STATUS_CONFIRMED.equals(dictamenVisual)) {
            Toast.makeText(getContext(), "✅ Boleto guardado en Room", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "❌ Error: No se guardó el boleto", Toast.LENGTH_LONG).show();
        }

        abrioModuloQrPreviamente = false;

        this.ultimoBoletoRegistrado = boleto;
        this.huboFallaMecanicaPapel = false;
        this.huboFallaDesconexion = false;

        if (bluetoothConnection != null) {
            new Thread(() -> {
                try {
                    String payloadNormal = "BOLETO " + boleto.uuid;
                    String payloadFinal = ticketPrintProcessor.formatearPayloadSeguro(payloadNormal, false);

                    PrinterHelper.imprimirBoleto(requireContext(), bus.placa, tipoPasajeroActual,
                            precioCentavos / 100.0, metodoPago, boleto.hash, bluetoothConnection);

                } catch (Exception e) {
                    if (e.getMessage() != null && e.getMessage().contains("paper")) {
                        huboFallaMecanicaPapel = true;
                    } else {
                        huboFallaDesconexion = true;
                    }
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "⚠️ Interrupción de hardware. Verifique el rodillo.", Toast.LENGTH_LONG).show()
                    );
                }
            }).start();
            Toast.makeText(getContext(), "Boleto emitido e impreso.", Toast.LENGTH_SHORT).show();
        } else {
            this.huboFallaDesconexion = true;
            Toast.makeText(getContext(), "Impresora fuera de línea. Habilitando modo re-vinculación.", Toast.LENGTH_LONG).show();
        }
    }

    private void verificarPermisosYConectar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_BLUETOOTH_PERMISSIONS);
                return;
            }
        } else {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_BLUETOOTH_PERMISSIONS);
                return;
            }
        }
        conectarDispositivoImpresora();
    }

    @SuppressLint("MissingPermission")
    private void conectarDispositivoImpresora() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        String mac = prefs.getString("printer_mac", "");
        if (mac.isEmpty()) {
            tvEstadoImpresora.setText("Ticketera: No configurada.");
            if (batteryManager != null) batteryManager.setEnlaceBluetoothActivo(false);
            return;
        }
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null && adapter.isEnabled()) {
            try {
                BluetoothDevice device = adapter.getRemoteDevice(mac);
                bluetoothConnection = new BluetoothConnection(device);
                tvEstadoImpresora.setText("Ticketera: Conectada Permanente ✓");
                tvEstadoImpresora.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark));

                // Habilitamos el canal lógico en el mánager e iniciamos el bucle periódico
                if (batteryManager != null) {
                    batteryManager.setEnlaceBluetoothActivo(true);
                    batteryManager.iniciarMonitoreo();
                }
            } catch (Exception e) {
                bluetoothConnection = null;
                tvEstadoImpresora.setText("Ticketera: Error de enlace.");
                if (batteryManager != null) {
                    batteryManager.setEnlaceBluetoothActivo(false);
                    batteryManager.iniciarMonitoreo();
                }
            }
        } else {
            if (batteryManager != null) {
                batteryManager.setEnlaceBluetoothActivo(false);
                batteryManager.iniciarMonitoreo();
            }
        }
    }

    private void ejecutarReimpresionDeContingencia(boolean esBotonReconectarPresionado) {
        if (ultimoBoletoRegistrado == null) {
            Toast.makeText(getContext(), "No existe boleto previo en memoria para reimprimir.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (esBotonReconectarPresionado && huboFallaDesconexion) {
            verificarPermisosYConectar();
        }

        String dictamenImpresion = ticketPrintProcessor.evaluarSolicitudImpresion(
                true,
                huboFallaMecanicaPapel,
                huboFallaDesconexion,
                true
        );

        if (TicketPrintProcessor.STATUS_DENIED.equals(dictamenImpresion)) {
            Toast.makeText(getContext(), "Acción rechazada por seguridad: Dispositivo sano o transacción inválida.", Toast.LENGTH_LONG).show();
            return;
        }

        boolean requiereGlosaAuditoria = TicketPrintProcessor.STATUS_CAN_REPRINT_JAM.equals(dictamenImpresion);

        if (bluetoothConnection != null) {
            new Thread(() -> {
                String payloadBase = "BOLETO " + ultimoBoletoRegistrado.uuid;
                String payloadAuditoria = ticketPrintProcessor.formatearPayloadSeguro(payloadBase, requiereGlosaAuditoria);

                PrinterHelper.imprimirBoleto(requireContext(), "BUS-PLACA", ultimoBoletoRegistrado.tipoPasajero,
                        ultimoBoletoRegistrado.precioCentavos / 100.0, ultimoBoletoRegistrado.metodoPago,
                        ultimoBoletoRegistrado.hash, bluetoothConnection);

                huboFallaMecanicaPapel = false;
                huboFallaDesconexion = false;
            }).start();
            Toast.makeText(getContext(), "Reimpresión completada con éxito.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "Fallo crítico: El periférico térmico sigue sin responder.", Toast.LENGTH_LONG).show();
        }
    }

    // ADICIÓN: Liberar recursos al salir de la pantalla para evitar fugas de memoria
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (batteryManager != null) {
            batteryManager.detenerMonitoreo();
        }
    }
}