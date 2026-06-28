package com.alexander.pasajes.network;

import android.content.Context;
import android.content.SharedPreferences;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.concurrent.TimeUnit;

public class RetrofitClient {
    private static final String BASE_URL = "https://svdrayf-backend.onrender.com/";
    private static Retrofit retrofit = null;
    private static ApiService apiService = null;

    public static ApiService getApiService(Context context) {
        if (apiService == null) {
            // 🚀 CORREGIDO: Forzar ApplicationContext para prevenir fugas de memoria en cambios de pantalla
            final Context appContext = context.getApplicationContext();
            final SharedPreferences prefs = appContext.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);

            // 🔒 BLINDAJE CONTRA EL COLD-START DE RENDER (Plan Gratuito tarda en despertar)
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(90, TimeUnit.SECONDS) // Espera hasta 90s a que Render encienda la máquina
                    .readTimeout(90, TimeUnit.SECONDS)    // Espera hasta 90s a que Neon DB responda
                    .writeTimeout(90, TimeUnit.SECONDS)
                    .addInterceptor(chain -> {
                        // Leer el token guardado en SharedPreferences EN TIEMPO REAL por cada request
                        String token = prefs.getString("token", null);
                        Request original = chain.request();

                        Request.Builder builder = original.newBuilder()
                                .header("Content-Type", "application/json")
                                .header("Accept", "application/json"); // Forzar respuesta estructurada limpia

                        if (token != null && !token.isEmpty()) {
                            builder.header("Authorization", "Bearer " + token);
                        }

                        return chain.proceed(builder.build());
                    })
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            apiService = retrofit.create(ApiService.class);
        }
        return apiService;
    }
}