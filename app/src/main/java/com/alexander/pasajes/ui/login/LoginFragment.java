package com.alexander.pasajes.ui.login; // 🟢 Un solo package al inicio del documento

import android.content.Context;
import android.content.Intent;
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
// Corrección: Importación unificada de todos los modelos del paquete de red
import com.alexander.pasajes.network.model.*;
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

    private final LoginAuthProcessor authProcessor = new LoginAuthProcessor();

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

        tvForgotPassword.setOnClickListener(v -> {
            String identificador = etIdentificador.getText().toString().trim();

            if (identificador.isEmpty()) {
                Toast.makeText(getContext(), "Por favor, ingrese su correo o DNI en el campo de identificación primero.", Toast.LENGTH_SHORT).show();
                return;
            }

            ResetForcedRequest requestRecover = new ResetForcedRequest(identificador, null, null);

            ApiService api = RetrofitClient.getApiService(requireContext());
            api.solicitarRecuperacion(requestRecover).enqueue(new Callback<GenericResponse>() {
                @Override
                public void onResponse(@NonNull Call<GenericResponse> call, @NonNull Response<GenericResponse> response) {
                    if (response.isSuccessful() && response.body() != null && "OK".equals(response.body().getStatus())) {
                        Toast.makeText(getContext(), "Solicitud registrada con éxito. Comuníquese con el Administrador para obtener su PIN temporal de acceso.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getContext(), "Error: El identificador ingresado no pertenece al personal autorizado.", Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<GenericResponse> call, @NonNull Throwable t) {
                    Toast.makeText(getContext(), "Error de red: Verifique su conexión a internet.", Toast.LENGTH_SHORT).show();
                }
            });
        });
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

                        if (loginResp.getUsuario().isRequiereCambio()) {
                            btnLogin.setEnabled(true);
                            Intent intent = new Intent(getContext(), ResetPasswordActivity.class);
                            intent.putExtra("identificador_usuario", identificador);
                            // Corrección: Envía el token JWT requerido por ResetPasswordActivity para firmar la petición
                            intent.putExtra("token_jwt", loginResp.getToken());
                            startActivity(intent);
                            return;
                        }

                        SharedPreferences prefs = requireActivity().getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
                        prefs.edit()
                                .putString("token", loginResp.getToken())
                                .putInt("id_usuario", loginResp.getUsuario().getIdUsuario())
                                .putInt("id_rol", loginResp.getUsuario().getIdRol())
                                .putString("nombres", loginResp.getUsuario().getNombres())
                                .apply();

                        Usuario usuario = new Usuario();
                        usuario.id = loginResp.getUsuario().getIdUsuario();
                        usuario.username = identificador;
                        usuario.password = password;
                        usuario.rol = loginResp.getUsuario().getIdRol();
                        usuario.nombres = loginResp.getUsuario().getNombres();
                        repo.insertOrUpdateUsuario(usuario);

                        if (listener != null) {
                            listener.onLoginSuccess(usuario.id, usuario.rol, loginResp.getToken());
                        }
                    } else {
                        Toast.makeText(getContext(), LoginAuthProcessor.MSG_ERROR_CREDENTIALS, Toast.LENGTH_LONG).show();
                    }
                }
                else if (response.code() == 401 || response.code() == 403) {
                    Toast.makeText(getContext(), LoginAuthProcessor.MSG_ERROR_CREDENTIALS, Toast.LENGTH_LONG).show();
                }
                else {
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
        boolean usuarioLocalEncontrado = (user != null);

        String dictamen = authProcessor.evaluarEstadoAutenticacion(false, false, usuarioLocalEncontrado, !usuarioLocalEncontrado);

        if (LoginAuthProcessor.MSG_SUCCESS_OFFLINE.equals(dictamen) && user != null) {
            SharedPreferences prefs = requireActivity().getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
            prefs.edit()
                    .putInt("id_usuario", user.id)
                    .putInt("id_rol", user.rol)
                    .putString("nombres", user.nombres)
                    .apply();

            if (listener != null) {
                listener.onLoginSuccess(user.id, user.rol, null);
            }
            Toast.makeText(getContext(), "Operando en Modo Offline", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), dictamen, Toast.LENGTH_LONG).show();
        }
    }
}