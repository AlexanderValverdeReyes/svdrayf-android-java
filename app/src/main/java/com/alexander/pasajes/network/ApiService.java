package com.alexander.pasajes.network;

import com.alexander.pasajes.network.model.*;
import retrofit2.Call;
import retrofit2.http.*;

public interface ApiService {

    @POST("api/auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @GET("api/maestros/sync")
    Call<MaestrosResponse> getMaestros();

    @POST("api/operacion/turno/apertura")
    Call<TurnoAperturaResponse> abrirTurno(@Body TurnoAperturaRequest request);

    @POST("api/operacion/turno/cierre")
    Call<GenericResponse> cerrarTurno(@Body TurnoCierreRequest request);

    @POST("api/sync/boletos")
    Call<GenericResponse> syncBoletos(@Body SyncBoletosRequest request);

    @POST("api/sync/incidencias")
    Call<GenericResponse> syncIncidencias(@Body IncidenciaRequest request);

    @POST("api/fiscalizacion/inspeccion")
    Call<GenericResponse> inspeccion(@Body IncidenciaRequest request);

    @GET("api/admin/buscar-boletos")
    Call<BuscarBoletoResponse> buscarBoletoPorHash(@Query("hash") String hash);

    @GET("api/fiscalizacion/verificar-boleto")
    Call<BuscarBoletoResponse> verificarBoleto(@Query("hash") String hash);

    // Enviar identificador a la cola de soporte del Administrador
    @POST("api/auth/recover")
    Call<GenericResponse> solicitarRecuperacion(@Body ResetForcedRequest request);

    // Actualización definitiva de clave protegida por JWT (Consume el flag requiere_cambio)
    @POST("api/auth/change-forced-password")
    Call<GenericResponse> changeForcedPassword(
            @Header("Authorization") String bearerToken,
            @Body ResetForcedRequest request
    );
}