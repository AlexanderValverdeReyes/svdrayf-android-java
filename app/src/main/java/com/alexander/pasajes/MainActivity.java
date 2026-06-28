package com.alexander.pasajes;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.alexander.pasajes.ui.login.LoginFragment;
import com.alexander.pasajes.ui.cobrador.CobradorMainFragment;
import com.alexander.pasajes.ui.inspector.InspectorMainFragment;
import com.alexander.pasajes.ui.sale.SaleFragment;
import com.alexander.pasajes.ui.history.HistoryFragment;
import com.alexander.pasajes.ui.printer.PrinterSetupFragment;
import java.util.List;
import com.alexander.pasajes.data.entity.Ruta;
import com.alexander.pasajes.data.entity.Turno;
import com.alexander.pasajes.repository.AppRepository;

public class MainActivity extends AppCompatActivity implements
        LoginFragment.OnLoginSuccessListener,
        PrinterSetupFragment.OnPrinterSetupListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            LoginFragment loginFragment = new LoginFragment();
            loginFragment.setOnLoginSuccessListener(this);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, loginFragment)
                    .commit();
        }
    }

    @Override
    public void onLoginSuccess(int idUsuario, int idRol, String token) {
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        prefs.edit()
                .putInt("id_usuario", idUsuario)
                .putInt("id_rol", idRol)
                .putString("token", token)
                .apply();

        if (idRol == 5) { // Cobrador
            AppRepository repo = new AppRepository(this);

            // 🚀 CORREGIDO: Busca el turno activo correspondiente estrictamente a este usuario
            Turno turnoActivo = repo.getTurnoActivoPorVendedor(idUsuario);

            if (turnoActivo != null && turnoActivo.activo) {
                Toast.makeText(this, "🔄 Recuperando tu jornada activa en curso...", Toast.LENGTH_SHORT).show();
                String tipoRuta = obtenerTipoRuta(turnoActivo.rutaId);
                irAVenta(turnoActivo.id, tipoRuta, turnoActivo.rutaId, turnoActivo.busId);
            } else {
                // Ir de forma secuencial y obligatoria a la ticketera antes de abrir turno
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new PrinterSetupFragment())
                        .commit();
            }
        } else if (idRol == 4) { // Fiscalizador
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new InspectorMainFragment())
                    .commit();
        }
    }

    private String obtenerTipoRuta(int rutaId) {
        AppRepository repo = new AppRepository(this);
        List<Ruta> rutas = repo.getRutas();
        if (rutas != null) {
            for (Ruta r : rutas) {
                if (r.id == rutaId) return r.tipo;
            }
        }
        return "DIRECTO";
    }

    @Override
    public void onPrinterSetupComplete() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new CobradorMainFragment())
                .commit();
    }

    public void irAVenta(long turnoLocalId, String tipoRuta, int rutaId, int busId) {
        SaleFragment saleFragment = new SaleFragment();
        Bundle args = new Bundle();
        args.putLong("turnoId", turnoLocalId);
        args.putString("tipoRuta", tipoRuta);
        args.putInt("rutaId", rutaId);
        args.putInt("busId", busId);
        saleFragment.setArguments(args);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, saleFragment)
                .addToBackStack(null)
                .commit();
    }

    public void irAHistorial(int turnoId) {
        HistoryFragment historyFragment = new HistoryFragment();
        historyFragment.setTurnoId(turnoId);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, historyFragment)
                .addToBackStack(null)
                .commit();
    }
}