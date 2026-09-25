package com.example.httpclientreval.ui;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/** Atajos de teclado de la app: Ctrl (Cmd en macOS) + Enter / K / L / G / D. */
final class Atajos {

    static final KeyStroke ENVIAR = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, mascara());
    static final KeyStroke BUSCAR = KeyStroke.getKeyStroke(KeyEvent.VK_K, mascara());
    static final KeyStroke LIMPIAR = KeyStroke.getKeyStroke(KeyEvent.VK_L, mascara());
    static final KeyStroke GENERAR = KeyStroke.getKeyStroke(KeyEvent.VK_G, mascara());
    static final KeyStroke FAVORITO = KeyStroke.getKeyStroke(KeyEvent.VK_D, mascara());

    private Atajos() {
    }

    private static int mascara() {
        return Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
    }

    /**
     * Asocia el atajo a la acción mientras el componente esté en la ventana
     * activa, sin importar qué campo tenga el foco. Al quitar el componente de
     * la ventana (p. ej. al cambiar de panel) el atajo deja de aplicar.
     */
    static void registrar(JComponent componente, KeyStroke atajo, Runnable accion) {
        Object clave = "atajo-" + atajo;
        componente.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(atajo, clave);
        componente.getActionMap().put(clave, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                accion.run();
            }
        });
    }

    /** Texto legible del atajo, ej. "Ctrl+Enter", para tooltips. */
    static String texto(KeyStroke atajo) {
        String modificador = (atajo.getModifiers() & KeyEvent.META_DOWN_MASK) != 0 ? "Cmd" : "Ctrl";
        return modificador + "+" + KeyEvent.getKeyText(atajo.getKeyCode());
    }
}
