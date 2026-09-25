package com.example.httpclientreval.ui;

import com.example.httpclientreval.model.EnvioRegistrado;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Historial de envíos al WS de la sesión actual, del más nuevo al más antiguo.
 * Vive solo en memoria (no se escribe a disco, porque la petición lleva las
 * credenciales WS-Security) y se pierde al cerrar la app. Se usa desde el hilo
 * de la UI.
 */
final class HistorialEnvios {

    static final int MAXIMO = 200;

    private static final HistorialEnvios INSTANCIA = new HistorialEnvios();

    private final List<EnvioRegistrado> envios = new ArrayList<>();
    private final List<Runnable> oyentes = new ArrayList<>();

    private HistorialEnvios() {
    }

    static HistorialEnvios instancia() {
        return INSTANCIA;
    }

    /** Envíos del más nuevo al más antiguo (vista de solo lectura). */
    List<EnvioRegistrado> envios() {
        return Collections.unmodifiableList(envios);
    }

    void agregar(EnvioRegistrado envio) {
        envios.add(0, envio);
        while (envios.size() > MAXIMO) {
            envios.remove(envios.size() - 1);
        }
        avisar();
    }

    void vaciar() {
        envios.clear();
        avisar();
    }

    void alCambiar(Runnable oyente) {
        oyentes.add(oyente);
    }

    private void avisar() {
        for (Runnable oyente : new ArrayList<>(oyentes)) {
            oyente.run();
        }
    }
}
