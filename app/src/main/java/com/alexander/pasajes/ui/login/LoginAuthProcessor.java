package com.alexander.pasajes.ui.login;

public class LoginAuthProcessor {

    public static final String MSG_SUCCESS_ONLINE = "ONLINE_SUCCESS";
    public static final String MSG_SUCCESS_OFFLINE = "OFFLINE_SUCCESS";
    public static final String MSG_ERROR_CREDENTIALS = "Error de autenticación: El usuario o la contraseña introducida son incorrectos. Verifique sus datos e intente nuevamente";
    public static final String MSG_ERROR_NO_SYNC = "No se puede iniciar sesión en Modo Offline. Se requiere una conexión inicial a internet para sincronizar su perfil de seguridad por primera vez en este terminal";

    public String evaluarEstadoAutenticacion(boolean esOnline, boolean apiSuccess, boolean usuarioLocalEncontrado, boolean baseDatosLocalVacia) {
        if (esOnline) {
            if (apiSuccess) {
                return MSG_SUCCESS_ONLINE;
            } else {
                return MSG_ERROR_CREDENTIALS;
            }
        } else {
            if (baseDatosLocalVacia) {
                return MSG_ERROR_NO_SYNC;
            } else if (usuarioLocalEncontrado) {
                return MSG_SUCCESS_OFFLINE;
            } else {
                return MSG_ERROR_CREDENTIALS;
            }
        }
    }
}