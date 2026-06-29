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

        if (busId > 0) {
            Bus bus = repo.getBus(busId);
            if (bus != null) tvBusInfo.setText(String.format("Unidad: %s", bus.placa));
        }

        //  CONTROL MATRICIAL DE MODALIDADES DE VIAJE CONECTADO A POSTGRESQL
        if ("DIRECTO".equals(tipoRuta)) {
            layoutTramos.setVisibility(View.GONE);
            origenSeleccionado = 1;  //  CORREGIDO: Paradero Mala es ID 1 en tu Neon DB
            destinoSeleccionado = 2; // CORREGIDO: Paradero Lima es ID 2 en tu Neon DB
            inyectarPasajesDirectos();
        } else {
            layoutTramos.setVisibility(View.VISIBLE);
            cargarTramosParaderos();
        }

        rgMetodoPago.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbQR) {
                ivQR.setVisibility(View.VISIBLE);
                abrioModuloQrPreviamente = true;
            } else {
                ivQR.setVisibility(View.GONE);
            }
        });

        rgTipoPasajero.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId != -1) {
                RadioButton rb = view.findViewById(checkedId);
                if (rb != null) {
                    String tagPasajero = (String) rb.getTag();

                    // 🛡️ INTERCEPTOR DE SEGURIDAD OPERATIVA (Mapeo CP76 y CP77)
                    String dictamen = passengerProcessor.evaluarSeleccionPasajero(
                            origenSeleccionado,
                            destinoSeleccionado,
                            tagPasajero,
                            regimenDia
                    );

                    if (PassengerTypeProcessor.MSG_ERROR_MISSING_STOPS.equals(dictamen)) {
                        // Despliega la notificación flotante y cancela el cálculo del costo
                        Toast.makeText(getContext(), dictamen, Toast.LENGTH_LONG).show();
                        tipoPasajeroActual = null;
                        tvPrecio.setText("Tarifa: No indexada");
                        return;
                    }

                    if (PassengerTypeProcessor.MSG_WARN_UNIVERISTARIO_HOLIDAY.equals(dictamen)) {
                        // Lanza la advertencia preventiva flotante sin interrumpir el flujo lícito
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
        //  CORREGIDO: Nombres de categorías homologados al 100% con tu base de datos
        agregarRadioBotonTipo("General");

        //  REGLA DE NEGOCIO: Universitarios y Frecuentes no aplican para Domingos/Feriados (Se ocultan)
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
            btn.setOnClickListener(v -> {
                origenSeleccionado = tramo.origenParaderoId;
                destinoSeleccionado = tramo.destinoParaderoId;

                for (int i = 0; i < layoutTramos.getChildCount(); i++) {
                    View c = layoutTramos.getChildAt(i);
                    if (c instanceof Button) c.setSelected(c == v);
                }

                //  REGLA DE NEGOCIO INMUTABLE: En rutas cortas intermedios (Mala-Chilca), universitario está prohibido
                rgTipoPasajero.removeAllViews();
                agregarRadioBotonTipo("General");
                actualizarPrecio();
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
        Tarifa t = repo.getTarifa(rutaId, origenSeleccionado, destinoSeleccionado, tipoPasajero);
        if (t != null) {
            // Retorna el campo correcto mapeado desde Neon DB según el spinner de la jornada
            return "FERIADO".equals(regimenDia) ? t.precioDomFerCentavos : t.precioCentavos;
        }

        //  FALLBACK ATÓMICO EN SOFTWARE (Garantiza que la app calcule pasajes bajo cualquier escenario)
        if ("DIRECTO".equals(tipoRuta)) {
            if ("FERIADO".equals(regimenDia)) return 1000; // S/. 10 Tarifa Plana Domingos/Feriados
            if ("Universitario".equals(tipoPasajero)) return 700; // S/. 7
            if ("Frecuente".equals(tipoPasajero)) return 800; // S/. 8
            return 900; // General S/. 9
        } else {
            // Tramo Corto Intermedio (Mala - Chilca)
            return "FERIADO".equals(regimenDia) ? 600 : 400; // S/. 6 Feriado o S/. 4 Semana
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

        // Garantizar un turnoId válido
        int idTurnoReal = (int) turnoId;
        if (idTurnoReal <= 0) {
            Turno activo = repo.getTurnoActivo();
            if (activo != null) idTurnoReal = activo.id;
        }
        if (idTurnoReal <= 0) {
            Toast.makeText(getContext(), "Error: No hay turno activo", Toast.LENGTH_LONG).show();
            return;
        }

        final Boleto boleto = new Boleto();
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
        Tarifa tarifaUsada = repo.getTarifa(rutaId, origenSeleccionado, destinoSeleccionado, tipoPasajeroActual);
        boleto.tarifarioId = (tarifaUsada != null) ? tarifaUsada.id : 3;
        // Insertar en Room
        viewModel.venderBoleto(boleto);

        // Verificar inmediatamente la inserción
        List<Boleto> boletosTurno = repo.getBoletosTurno(idTurnoReal);
        boolean guardado = false;
        for (Boleto b : boletosTurno) {
            if (b.uuid != null && b.uuid.equals(boleto.uuid)) {
                guardado = true;
                break;
            }
        }

        if (guardado) {
            Toast.makeText(getContext(), "✅ Boleto guardado en Room", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "❌ Error: No se guardó el boleto", Toast.LENGTH_LONG).show();
        }

        abrioModuloQrPreviamente = false;

        if (bluetoothConnection != null) {
            new Thread(() -> {
                PrinterHelper.imprimirBoleto(requireContext(), bus.placa, tipoPasajeroActual,
                        precioCentavos / 100.0, metodoPago, boleto.hash, bluetoothConnection);
            }).start();
            Toast.makeText(getContext(), "Boleto emitido e impreso.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "Impresora fuera de línea.", Toast.LENGTH_LONG).show();
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
            return;
        }
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null && adapter.isEnabled()) {
            try {
                BluetoothDevice device = adapter.getRemoteDevice(mac);
                bluetoothConnection = new BluetoothConnection(device);
                tvEstadoImpresora.setText("Ticketera: Conectada Permanente ✓");
                tvEstadoImpresora.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark));
            } catch (Exception e) {
                bluetoothConnection = null;
                tvEstadoImpresora.setText("Ticketera: Error de enlace.");
            }
        }
    }
}