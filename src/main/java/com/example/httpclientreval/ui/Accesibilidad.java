package com.example.httpclientreval.ui;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.text.JTextComponent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/** Ayudas para los lectores de pantalla y la navegación con teclado. */
final class Accesibilidad {

    private Accesibilidad() {
    }

    /** Nombre que anuncia el lector de pantalla; imprescindible en botones con solo icono, tablas y áreas de texto. */
    static void nombrar(JComponent componente, String nombre) {
        componente.getAccessibleContext().setAccessibleName(nombre);
    }

    static void nombrar(JComponent componente, String nombre, String descripcion) {
        componente.getAccessibleContext().setAccessibleName(nombre);
        componente.getAccessibleContext().setAccessibleDescription(descripcion);
    }

    /**
     * Asocia la etiqueta con su campo (Alt+mnemónico, relación "etiquetado por" para los lectores de pantalla)
     * y le da al campo el nombre de la etiqueta.
     */
    static void etiquetar(JLabel etiqueta, JComponent campo, String nombre) {
        etiqueta.setLabelFor(campo);
        nombrar(campo, nombre);
    }

    /**
     * Hace que Tab y Mayús+Tab muevan el foco en un área de texto, en vez de insertar tabulaciones o quedar
     * atrapados en ella (en un área de solo lectura la tecla se consume igual y el teclado no puede salir).
     * Se hace con un KeyListener y no con el mapa de teclas porque éste se reemplaza al cambiar de tema.
     */
    static void tabulacionLibre(JTextComponent texto) {
        texto.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() != KeyEvent.VK_TAB || e.isControlDown() || e.isAltDown()) {
                    return;
                }
                e.consume();
                if (e.isShiftDown()) {
                    texto.transferFocusBackward();
                } else {
                    texto.transferFocus();
                }
            }

            @Override
            public void keyTyped(KeyEvent e) {
                // Sin esto el '\t' se inserta aunque keyPressed haya movido el foco.
                if (e.getKeyChar() == '\t') {
                    e.consume();
                }
            }
        });
    }
}
