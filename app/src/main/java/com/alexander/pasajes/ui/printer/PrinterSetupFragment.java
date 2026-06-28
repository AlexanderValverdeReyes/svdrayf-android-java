package com.alexander.pasajes.ui.printer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class PrinterSetupFragment extends Fragment {

    private BluetoothAdapter bluetoothAdapter;
    private ArrayAdapter<String> deviceAdapter;
    private ArrayList<BluetoothDevice> devicesList = new ArrayList<>();
    private ListView lvDevices;
    private TextView tvPrinterStatus;
    private Button btnContinue;
    private SharedPreferences prefs;

    // UUID Estándar Internacional para el Perfil de Puerto Serie (SPP) en Ticketeras ESC/POS
    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    public interface OnPrinterSetupListener {
        void onPrinterSetupComplete();
    }

    // 🚀 RECEPTOR DE SEÑALES DE RADIO: Escanea dispositivos nuevos y monitorea el emparejamiento
    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null && device.getName() != null) {
                    if (!devicesList.contains(device)) {
                        devicesList.add(device);
                        deviceAdapter.add(device.getName() + " (Nuevo)\n" + device.getAddress());
                        deviceAdapter.notifyDataSetChanged();
                    }
                }
            }
            // 🔒 ESCUCHO DE VINCULACIÓN: Si el usuario acepta el PIN de emparejamiento de Android
            else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);

                if (device != null && state == BluetoothDevice.BOND_BONDED) {
                    Toast.makeText(context, "✅ Vinculación confirmada. Probando conexión...", Toast.LENGTH_SHORT).show();
                    probarConexionFisicaReal(device);
                }
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_printer_setup, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        prefs = requireActivity().getSharedPreferences("app_prefs", Context.MODE_PRIVATE);

        lvDevices = view.findViewById(R.id.lvDevices);
        tvPrinterStatus = view.findViewById(R.id.tvPrinterStatus);

        Button btnScan = view.findViewById(R.id.btnScanDevices);
        Button btnSelect = view.findViewById(R.id.btnSelectPrinter);
        btnContinue = view.findViewById(R.id.btnContinueToSale);

        // Bloquear avance por defecto hasta que pase el ping real de hardware
        btnContinue.setEnabled(false);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(getContext(), "Hardware Bluetooth no disponible", Toast.LENGTH_LONG).show();
            return;
        }

        deviceAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_single_choice, new ArrayList<>());
        lvDevices.setAdapter(deviceAdapter);
        lvDevices.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        String savedPrinter = prefs.getString("printer_mac", "");
        if (!savedPrinter.isEmpty()) {
            tvPrinterStatus.setText("Última ticketera enlazada: " + savedPrinter);
            btnContinue.setEnabled(true); // Permitir omitir si ya se validó antes
        }

        btnScan.setOnClickListener(v -> verificarPermisosYBuscar());
        btnSelect.setOnClickListener(v -> vinculacionYEnlaceMapeado());

        btnContinue.setOnClickListener(v -> {
            if (getActivity() instanceof OnPrinterSetupListener) {
                ((OnPrinterSetupListener) getActivity()).onPrinterSetupComplete();
            }
        });
    }

    private void verificarPermisosYBuscar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT}, 100);
                return;
            }
        } else {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
                return;
            }
        }
        iniciarEscaneoRadar();
    }

    @SuppressLint("MissingPermission")
    private void iniciarEscaneoRadar() {
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }

        devicesList.clear();
        deviceAdapter.clear();

        // 1. Inyectar vinculados previos
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices != null) {
            for (BluetoothDevice device : pairedDevices) {
                devicesList.add(device);
                deviceAdapter.add(device.getName() + " (Vinculado previamente)\n" + device.getAddress());
            }
        }

        bluetoothAdapter.startDiscovery();
        Toast.makeText(getContext(), "Buscando ticketeras activas...", Toast.LENGTH_SHORT).show();
    }

    @SuppressLint("MissingPermission")
    private void vinculacionYEnlaceMapeado() {
        int pos = lvDevices.getCheckedItemPosition();
        if (pos == ListView.INVALID_POSITION) {
            Toast.makeText(getContext(), "Seleccione un dispositivo de la lista.", Toast.LENGTH_SHORT).show();
            return;
        }

        BluetoothDevice device = devicesList.get(pos);
        btnContinue.setEnabled(false);

        // 🛡️ COMPROBACIÓN REGLA DE HARDWARE: Si no está emparejado, forzar emparejamiento nativo
        if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
            Toast.makeText(getContext(), "Solicitando emparejamiento con la ticketera...", Toast.LENGTH_LONG).show();
            device.createBond(); // Dispara la tarjeta de emparejamiento de Android
        } else {
            // Si ya está emparejado, ir directo al ping de socket físico
            probarConexionFisicaReal(device);
        }
    }

    @SuppressLint("MissingPermission")
    private void probarConexionFisicaReal(BluetoothDevice device) {
        tvPrinterStatus.setText("Estableciendo handshake serial ESC/POS...");
        tvPrinterStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_orange_dark));

        // Abrimos un hilo secundario para que el intento de conexión no congele la pantalla
        new Thread(() -> {
            BluetoothSocket testSocket = null;
            boolean conexionExitosa = false;

            try {
                // Forzar la creación de un Socket RFCOMM bajo el perfil SPP estándar
                testSocket = device.createRfcommSocketToServiceRecord(SPP_UUID);
                bluetoothAdapter.cancelDiscovery(); // Apagar el radar para liberar ancho de banda

                testSocket.connect(); // 🔌 EL VERDADERO LLAMADO: Intenta abrir el canal físico
                conexionExitosa = true;

            } catch (IOException e) {
                conexionExitosa = false;
                try {
                    // Fallback de contingencia mediante reflexión por si es una ticketera genérica china
                    testSocket =(BluetoothSocket) device.getClass().getMethod("createRfcommSocket", new Class[] {int.class}).invoke(device,1);
                    testSocket.connect();
                    conexionExitosa = true;
                } catch (Exception ex) {
                    conexionExitosa = false;
                }
            } finally {
                if (testSocket != null) {
                    try {
                        testSocket.close(); // Cerrar el socket de prueba para no dejar el canal bloqueado
                    } catch (IOException ignored) {}
                }
            }

            // Devolver el veredicto del hardware al hilo de la interfaz de usuario
            final boolean resultado = conexionExitosa;
            new Handler(Looper.getMainLooper()).post(() -> {
                if (resultado) {
                    // Guardar en almacenamiento local únicamente si la conexión física es REAL
                    prefs.edit().putString("printer_mac", device.getAddress()).apply();
                    tvPrinterStatus.setText("Ticketera: Conectada y Lista ✓ (" + device.getAddress() + ")");
                    tvPrinterStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark));
                    btnContinue.setEnabled(true); // Destrabar flujo
                    Toast.makeText(getContext(), "Enlace verificado con éxito.", Toast.LENGTH_SHORT).show();
                } else {
                    tvPrinterStatus.setText("❌ Error de enlace físico. Verifique encendido.");
                    tvPrinterStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark));
                    btnContinue.setEnabled(false);
                    Toast.makeText(getContext(), "La ticketera rechazó el socket serial. Asegúrese de que esté en modo ESC/POS.", Toast.LENGTH_LONG).show();
                }
            });
        }).start();
    }

    @Override
    public void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        requireActivity().registerReceiver(bluetoothReceiver, filter);
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onStop() {
        super.onStop();
        if (bluetoothAdapter != null && bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        requireActivity().unregisterReceiver(bluetoothReceiver);
    }
}