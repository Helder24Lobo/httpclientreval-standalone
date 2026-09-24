package com.example.httpclientreval.ui;

import javax.swing.JFrame;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/** Recuerda el tamaño, la posición y si la ventana estaba maximizada entre corridas de la app. */
final class VentanaPreferencias {

    private static final Preferences PREFS = Preferences.userNodeForPackage(VentanaPreferencias.class);
    private static final String CLAVE_X = "ventana.x";
    private static final String CLAVE_Y = "ventana.y";
    private static final String CLAVE_ANCHO = "ventana.ancho";
    private static final String CLAVE_ALTO = "ventana.alto";
    private static final String CLAVE_MAXIMIZADA = "ventana.maximizada";

    /** Área mínima (px) que debe quedar visible en algún monitor para aceptar la posición guardada. */
    private static final int MINIMO_VISIBLE = 100;

    private VentanaPreferencias() {
    }

    /**
     * Aplica los límites guardados (si los hay y caen en un monitor
     * conectado) y deja la ventana guardándolos al cerrarse. Devuelve
     * {@code false} si no había nada válido que restaurar, para que el
     * llamador decida la posición inicial.
     */
    static boolean restaurarYRecordar(JFrame ventana) {
        boolean restaurada = restaurar(ventana);
        recordar(ventana);
        return restaurada;
    }

    private static boolean restaurar(JFrame ventana) {
        int ancho = PREFS.getInt(CLAVE_ANCHO, -1);
        int alto = PREFS.getInt(CLAVE_ALTO, -1);
        if (ancho <= 0 || alto <= 0) {
            return false;
        }
        Rectangle limites = new Rectangle(PREFS.getInt(CLAVE_X, 0), PREFS.getInt(CLAVE_Y, 0), ancho, alto);
        if (!esVisible(limites)) {
            return false;
        }
        ventana.setBounds(limites);
        if (PREFS.getBoolean(CLAVE_MAXIMIZADA, false)) {
            ventana.setExtendedState(ventana.getExtendedState() | Frame.MAXIMIZED_BOTH);
        }
        return true;
    }

    private static void recordar(JFrame ventana) {
        // Al maximizar, getBounds() devuelve el área del monitor; se guardan
        // los últimos límites "normales" para que al desmaximizar quede igual.
        Rectangle[] limitesNormales = {ventana.getBounds()};
        ventana.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                actualizar();
            }

            @Override
            public void componentMoved(ComponentEvent e) {
                actualizar();
            }

            private void actualizar() {
                if (ventana.getExtendedState() == Frame.NORMAL) {
                    limitesNormales[0] = ventana.getBounds();
                }
            }
        });
        ventana.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                guardar(limitesNormales[0], (ventana.getExtendedState() & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH);
            }
        });
    }

    private static void guardar(Rectangle limites, boolean maximizada) {
        PREFS.putInt(CLAVE_X, limites.x);
        PREFS.putInt(CLAVE_Y, limites.y);
        PREFS.putInt(CLAVE_ANCHO, limites.width);
        PREFS.putInt(CLAVE_ALTO, limites.height);
        PREFS.putBoolean(CLAVE_MAXIMIZADA, maximizada);
        try {
            // EXIT_ON_CLOSE termina el proceso justo después de este evento.
            PREFS.flush();
        } catch (BackingStoreException ignorada) {
            // No poder recordar la ventana no debe impedir cerrar la app.
        }
    }

    /** true si una parte razonable de la ventana cae dentro de algún monitor conectado. */
    private static boolean esVisible(Rectangle limites) {
        for (GraphicsDevice pantalla : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) {
            Rectangle interseccion = pantalla.getDefaultConfiguration().getBounds().intersection(limites);
            if (interseccion.width >= MINIMO_VISIBLE && interseccion.height >= MINIMO_VISIBLE) {
                return true;
            }
        }
        return false;
    }
}
