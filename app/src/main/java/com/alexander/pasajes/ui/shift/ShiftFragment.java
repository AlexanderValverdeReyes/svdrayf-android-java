package com.alexander.pasajes.ui.shift;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.alexander.pasajes.R;
import com.alexander.pasajes.data.entity.Bus;
import com.alexander.pasajes.data.entity.Turno;
import com.alexander.pasajes.repository.AppRepository;
import java.util.List;

public class ShiftFragment extends Fragment {

    private AppRepository repo;
    private int vendedorId;
    private OnShiftStartedListener listener;

    public interface OnShiftStartedListener {
        void onShiftStarted(int turnoId);
    }

    public void setOnShiftStartedListener(OnShiftStartedListener listener) {
        this.listener = listener;
    }

    public void setVendedorId(int vendedorId) {
        this.vendedorId = vendedorId;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_shift, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repo = new AppRepository(requireContext());
        Spinner spinnerBus = view.findViewById(R.id.spinnerBus);
        Button btnAbrirTurno = view.findViewById(R.id.btnAbrirTurno);

        List<Bus> buses = repo.getAllBuses();
        ArrayAdapter<Bus> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, buses);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBus.setAdapter(adapter);

        btnAbrirTurno.setOnClickListener(v -> {
            Bus bus = (Bus) spinnerBus.getSelectedItem();
            if (bus == null) return;

            Turno turno = new Turno();
            turno.vendedorId = vendedorId;
            turno.busId = bus.id;
            turno.apertura = System.currentTimeMillis();
            turno.cierre = 0;
            turno.activo = true;
            long turnoId = repo.abrirTurno(turno);
            Toast.makeText(getContext(), "Turno abierto con ID " + turnoId, Toast.LENGTH_SHORT).show();
            if (listener != null) listener.onShiftStarted((int) turnoId);
        });
    }
}