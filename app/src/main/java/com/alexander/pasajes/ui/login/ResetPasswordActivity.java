package com.alexander.pasajes.ui.login;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.alexander.pasajes.R;
import com.alexander.pasajes.network.ApiService;
import com.alexander.pasajes.network.RetrofitClient;
import com.alexander.pasajes.network.model.GenericResponse;
import com.alexander.pasajes.network.model.ResetForcedRequest;
import com.google.android.material.textfield.TextInputEditText;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ResetPasswordActivity extends AppCompatActivity {

    private TextInputEditText etNuevaPassword, etConfirmarPassword;
    private Button btnRestablecer;
    private String tokenJwt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        // Recuperar el token de sesión temporal enviado desde el LoginFragment
        tokenJwt = getIntent().getStringExtra("token_jwt");

        etNuevaPassword = findViewById(R.id.etNuevaPassword);
        etConfirmarPassword = findViewById(R.id.etConfirmarPassword);
        btnRestablecer = findViewById(R.id.btnRestablecer);

        // Nota: etTokenAdmin ya no se evalúa aquí porque la validación del PIN
        // o DNI provisional ya fue resuelta y autorizada por la API de Login.
        btnRestablecer.setOnClickListener(v -> procesarCambioContrasena());
    }

    private void procesarCambioContrasena() {
        String nuevaPass = etNuevaPassword.getText().toString().trim();
        String confirmarPass = etConfirmarPassword.getText().toString().trim();

        if (nuevaPass.isEmpty() || confirmarPass.isEmpty()) {
            Toast.makeText(this, "Debe completar todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        if (nuevaPass.length() < 6) {
            Toast.makeText(this, "La nueva contraseña debe contener un mínimo de 6 caracteres.", Toast.LENGTH_LONG).show();
            return;
        }

        if (!nuevaPass.equals(confirmarPass)) {
            Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_LONG).show();
            return;
        }

        btnRestablecer.setEnabled(false);
        ApiService api = RetrofitClient.getApiService(this);

        // Cabecera Bearer requerida por el authMiddleware del Backend en Node.js
        String authorizationHeader = "Bearer " + tokenJwt;

        // Reutilización segura: El servidor Render solo extraerá la propiedad "newPassword" del JSON
        ResetForcedRequest request = new ResetForcedRequest(null, null, nuevaPass);

        api.changeForcedPassword(authorizationHeader, request).enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(@NonNull Call<GenericResponse> call, @NonNull Response<GenericResponse> response) {
                btnRestablecer.setEnabled(true);
                if (response.isSuccessful() && response.body() != null && "OK".equals(response.body().getStatus())) {
                    Toast.makeText(ResetPasswordActivity.this, "Contraseña actualizada. Inicie sesión de nuevo.", Toast.LENGTH_LONG).show();
                    finish(); // Cierra la actividad de cambio y regresa al Login limpio
                } else {
                    Toast.makeText(ResetPasswordActivity.this, "Error: Contraseña no permitida (No puede ser igual al DNI) o sesión expirada.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<GenericResponse> call, @NonNull Throwable t) {
                btnRestablecer.setEnabled(true);
                Toast.makeText(ResetPasswordActivity.this, "Error de red: Sin comunicación con el servidor central", Toast.LENGTH_LONG).show();
            }
        });
    }
}