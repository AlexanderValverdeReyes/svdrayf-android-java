package com.alexander.pasajes.ui.sale;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import com.alexander.pasajes.data.entity.Boleto;
import com.alexander.pasajes.data.entity.Turno;
import com.alexander.pasajes.repository.AppRepository;

public class SaleViewModel extends AndroidViewModel {
    private final AppRepository repo;
    private Turno turnoActivo;

    public SaleViewModel(@NonNull Application app) {
        super(app);
        repo = new AppRepository(app);
        this.turnoActivo = repo.getTurnoActivo();
    }

    public Turno getTurnoActivo() {
        if (turnoActivo == null) {
            turnoActivo = repo.getTurnoActivo();
        }
        return turnoActivo;
    }

    public void venderBoleto(Boleto boleto) {
        // ✅ CORREGIDO: Conserva el turno analítico sin destruir la trazabilidad de Room
        if (turnoActivo != null) {
            boleto.turnoId = turnoActivo.id;
        }
        repo.venderBoleto(boleto);
    }
}