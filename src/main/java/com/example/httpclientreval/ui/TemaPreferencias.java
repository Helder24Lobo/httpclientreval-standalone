package com.example.httpclientreval.ui;

import java.util.prefs.Preferences;

/** Guarda el tema elegido (Claro/Oscuro) entre corridas de la app. */
public final class TemaPreferencias {

    public static final String CLARO = "Claro";
    public static final String OSCURO = "Oscuro";

    private static final Preferences PREFS = Preferences.userNodeForPackage(TemaPreferencias.class);
    private static final String CLAVE_TEMA = "tema";

    private TemaPreferencias() {
    }

    public static String obtenerTema() {
        return PREFS.get(CLAVE_TEMA, OSCURO);
    }

    public static void guardarTema(String tema) {
        PREFS.put(CLAVE_TEMA, tema);
    }
}
