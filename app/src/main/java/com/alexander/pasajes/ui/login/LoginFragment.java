package com.alexander.pasajes.ui.login;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.alexander.pasajes.R;
import com.alexander.pasajes.data.entity.Usuario;
import com.alexander.pasajes.network.ApiService;
import com.alexander.pasajes.network.RetrofitClient;
import com.alexander.pasajes.network.model.LoginRequest;
import com.alexander.pasajes.network.model.LoginResponse;
import com.alexander.pasajes.repository.AppRepository;
import com.google.android.material.textfield.TextInputEditText;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginFragment extends Fragment {

    private TextInputEditText etIdentificador, etPassword;
    private Button btnLogin;
    private TextView tvForgotPassword;
    private AppRepository repo;
    private OnLoginSuccessListener listener;

    public interface OnLoginSuccessListener {
        void onLoginSuccess(int idUsuario, int idRol, String token);
    }

    public void setOnLoginSuccessListener(OnLoginSuccessListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repo = new AppRepository(requireContext());
        etIdentificador = view.findViewById(R.id.etIdentificador);
        etPassword = view.findViewById(R.id.etPassword);
        btnLogin = view.findViewById(R.id.btnLogin);
        tvForgotPassword = view.findViewById(R.id.tvForgotPassword);

        btnLogin.setOnClickListener(v -> realizarLogin());
        tvForgotPassword.setOnClickListener(v ->
                Toast.makeText(getContext(), "Funcionalidad de recuperación en desarrollo", Toast.LENGTH_SHORT).show()
        );
    }

    private void realizarLogin() {
        final String identificador = etIdentificador.getText().toString().trim();
        final String password = etPassword.getText().toString().trim();

        if (identificador.isEmpty() || password.isEmpty()) {
            Toast.makeText(getContext(), "Por favor, complete todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        btnLogin.setEnabled(false);
        ApiService api = RetrofitClient.getApiService(requireContext());
        api.login(new LoginRequest(identificador, password)).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(@NonNull Call<LoginResponse> call, @NonNull Response<LoginResponse> response) {
                btnLogin.setEnabled(true);
                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse loginResp = response.body();
                    if ("OK".equals(loginResp.getStatus()) && loginResp.getUsuario() != null) {
                        // Guardar en SharedPreferences
                        SharedPreferences prefs = requireActivity().getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
                        prefs.edit()
                                .putString("token", loginResp.getToken())
                                .putInt("id_usuario", loginResp.getUsuario().getIdUsuario())
                                .putInt("id_rol", loginResp.getUsuario().getIdRol())
                                .putString("nombres", loginResp.getUsuario().getNombres())
                                .apply();

                        // Guardar en Room para offline
                        Usuario usuario = new Usuario();
                        usuario.id = loginResp.getUsuario().getIdUsuario();
                        usuario.username = identificador; // Guardamos el identificador tal cual (DNI, correo, etc.)
                        usuario.password = password;
                        usuario.rol = loginResp.getUsuario().getIdRol();
                        usuario.nombres = loginResp.getUsuario().getNombres();
                        repo.insertOrUpdateUsuario(usuario);

                        if (listener != null) {
                            listener.onLoginSuccess(usuario.id, usuario.rol, loginResp.getToken());
                        }
                    } else {
                        Toast.makeText(getContext(), "Usuario o contraseña incorrectos", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // Fallo de red, intentar offline
                    loginOffline(identificador, password);
                }
            }

            @Override
            public void onFailure(@NonNull Call<LoginResponse> call, @NonNull Throwable t) {
                btnLogin.setEnabled(true);
                loginOffline(identificador, password);
            }
        });
    }

    private void loginOffline(String identificador, String password) {
        Usuario user = repo.loginLocal(identificador, password);
        if (user != null) {
            // Guardar en SharedPreferences para mantener sesión
            SharedPreferences prefs = requireActivity().getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
            prefs.edit()
                    .putInt("id_usuario", user.id)
                    .putInt("id_rol", user.rol)
                    .putString("nombres", user.nombres)
                    .apply();

            if (listener != null) {
                listener.onLoginSuccess(user.id, user.rol, null);
            }
        } else {
            Toast.makeText(getContext(), "Credenciales incorrectas o sin conexión.", Toast.LENGTH_LONG).show();
        }
    }
}