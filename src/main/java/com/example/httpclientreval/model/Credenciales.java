package com.example.httpclientreval.model;

/**
 * Las credenciales con las que se cifra y se firma una petición: salen del ambiente activo. Los campos
 * del mismo nombre que traen los perfiles (de versiones anteriores, cuando las credenciales vivían en
 * cada perfil) solo se usan como respaldo si el ambiente no tiene las suyas.
 */
public final class Credenciales {

    private Credenciales() {
    }

    public static String llave(Profile perfil) {
        return llave(perfil, SoapHttpClient.entornoActivo());
    }

    public static String usuario(Profile perfil) {
        return usuario(perfil, SoapHttpClient.entornoActivo());
    }

    public static String password(Profile perfil) {
        return password(perfil, SoapHttpClient.entornoActivo());
    }

    public static String llave(Profile perfil, Entorno entorno) {
        return elegir(entorno.llaveAes, perfil.llaveAes);
    }

    public static String usuario(Profile perfil, Entorno entorno) {
        return elegir(entorno.wsseUsername, perfil.wsseUsername);
    }

    public static String password(Profile perfil, Entorno entorno) {
        return elegir(entorno.wssePassword, perfil.wssePassword);
    }

    private static String elegir(String delEntorno, String delPerfil) {
        return Entorno.esVacio(delEntorno) ? delPerfil : delEntorno;
    }
}
