package com.example.httpclientreval.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Guarda entre corridas los perfiles favoritos y los usados recientemente,
 * por nombre de perfil. Viven en las preferencias del usuario y no en
 * profiles.json, que contiene las llaves y se reescribe entero al guardar.
 */
final class PerfilesPreferencias {

    static final int MAX_RECIENTES = 8;

    private static final String CLAVE_FAVORITOS = "perfiles.favoritos";
    private static final String CLAVE_RECIENTES = "perfiles.recientes";
    private static final String SEPARADOR = "\n";

    private static final PerfilesPreferencias INSTANCIA =
            new PerfilesPreferencias(Preferences.userNodeForPackage(PerfilesPreferencias.class));

    private final Preferences prefs;

    PerfilesPreferencias(Preferences prefs) {
        this.prefs = prefs;
    }

    static PerfilesPreferencias instancia() {
        return INSTANCIA;
    }

    /** Favoritos, en el orden en que se marcaron. */
    List<String> favoritos() {
        return leer(CLAVE_FAVORITOS);
    }

    /** Recientes, del más nuevo al más antiguo. */
    List<String> recientes() {
        return leer(CLAVE_RECIENTES);
    }

    boolean esFavorito(String nombre) {
        return favoritos().contains(nombre);
    }

    /** Marca o desmarca el favorito; devuelve el estado resultante (true = ahora es favorito). */
    boolean alternarFavorito(String nombre) {
        List<String> lista = favoritos();
        boolean ahoraFavorito = !lista.remove(nombre);
        if (ahoraFavorito) {
            lista.add(nombre);
        }
        escribir(CLAVE_FAVORITOS, lista);
        return ahoraFavorito;
    }

    /** Pone el perfil de primero en recientes y descarta los más antiguos. */
    void registrarReciente(String nombre) {
        List<String> lista = recientes();
        lista.remove(nombre);
        lista.add(0, nombre);
        while (lista.size() > MAX_RECIENTES) {
            lista.remove(lista.size() - 1);
        }
        escribir(CLAVE_RECIENTES, lista);
    }

    /** Conserva favorito y reciente cuando se renombra un perfil. */
    void renombrar(String anterior, String nuevo) {
        for (String clave : new String[]{CLAVE_FAVORITOS, CLAVE_RECIENTES}) {
            List<String> lista = leer(clave);
            int posicion = lista.indexOf(anterior);
            if (posicion >= 0) {
                lista.set(posicion, nuevo);
                escribir(clave, lista);
            }
        }
    }

    /** Olvida un perfil que se eliminó. */
    void eliminar(String nombre) {
        for (String clave : new String[]{CLAVE_FAVORITOS, CLAVE_RECIENTES}) {
            List<String> lista = leer(clave);
            if (lista.remove(nombre)) {
                escribir(clave, lista);
            }
        }
    }

    private List<String> leer(String clave) {
        String valor = prefs.get(clave, "");
        List<String> lista = new ArrayList<>();
        if (!valor.isEmpty()) {
            for (String nombre : valor.split(SEPARADOR)) {
                if (!nombre.isEmpty() && !lista.contains(nombre)) {
                    lista.add(nombre);
                }
            }
        }
        return lista;
    }

    private void escribir(String clave, List<String> lista) {
        if (lista.isEmpty()) {
            prefs.remove(clave);
        } else {
            prefs.put(clave, String.join(SEPARADOR, lista));
        }
    }
}
