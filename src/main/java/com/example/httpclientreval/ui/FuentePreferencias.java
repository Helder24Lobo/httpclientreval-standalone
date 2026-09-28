package com.example.httpclientreval.ui;

import com.formdev.flatlaf.FlatLaf;

import java.util.Collections;
import java.util.prefs.Preferences;

/**
 * Tamaño de fuente de toda la interfaz, como porcentaje del tamaño por defecto
 * (100 %), recordado entre corridas. FlatLaf escala todas las fuentes de los
 * componentes a partir de {@code defaultFont}; los textos en negrita o con otro
 * tamaño se definen con estilos relativos (p. ej. "font: bold +2"), así que
 * también se ajustan al cambiar el porcentaje.
 */
final class FuentePreferencias {

    static final int[] PORCENTAJES = {90, 100, 115, 130, 150, 175, 200};
    static final int POR_DEFECTO = 100;

    private static final Preferences PREFS = Preferences.userNodeForPackage(FuentePreferencias.class);
    private static final String CLAVE = "fuente.porcentaje";

    private FuentePreferencias() {
    }

    /** Porcentaje guardado; si el valor no es uno de los permitidos (p. ej. editado a mano) se usa el 100 %. */
    static int obtener() {
        int guardado = PREFS.getInt(CLAVE, POR_DEFECTO);
        return indiceDe(guardado) >= 0 ? guardado : POR_DEFECTO;
    }

    static void guardar(int porcentaje) {
        PREFS.putInt(CLAVE, porcentaje);
    }

    /**
     * Deja el tamaño elegido listo para el próximo look and feel que se instale
     * (no cambia lo que ya está en pantalla: hay que reinstalar el LaF y llamar a
     * {@link FlatLaf#updateUI()}).
     */
    static void aplicar() {
        int porcentaje = obtener();
        FlatLaf.setGlobalExtraDefaults(porcentaje == POR_DEFECTO
                ? null : Collections.singletonMap("defaultFont", porcentaje + "%"));
    }

    /** Porcentaje {@code pasos} escalones por encima (positivo) o por debajo (negativo) del actual, sin salirse de la lista. */
    static int desplazar(int actual, int pasos) {
        int indice = indiceDe(actual);
        if (indice < 0) {
            indice = indiceDe(POR_DEFECTO);
        }
        int nuevo = Math.max(0, Math.min(PORCENTAJES.length - 1, indice + pasos));
        return PORCENTAJES[nuevo];
    }

    private static int indiceDe(int porcentaje) {
        for (int i = 0; i < PORCENTAJES.length; i++) {
            if (PORCENTAJES[i] == porcentaje) {
                return i;
            }
        }
        return -1;
    }
}
