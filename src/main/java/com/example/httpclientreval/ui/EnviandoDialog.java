package com.example.httpclientreval.ui;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Window;

/**
 * Ventana modal chica con un ícono de carga animado, para mostrar mientras
 * se espera la respuesta de una operación larga (ej. el envío al WS).
 * Uso: crearla, arrancar la tarea en segundo plano, y en su callback de
 * finalización llamar dispose() — eso desbloquea el setVisible(true) modal.
 */
public class EnviandoDialog extends JDialog {

    private final Timer animacion;

    public EnviandoDialog(Window propietario, String mensaje) {
        super(propietario, "Enviando", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        setResizable(false);

        Spinner spinner = new Spinner();
        spinner.setPreferredSize(new Dimension(36, 36));

        JPanel contenido = new JPanel(new BorderLayout(16, 0));
        contenido.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 32));
        contenido.add(spinner, BorderLayout.WEST);
        contenido.add(new JLabel(mensaje), BorderLayout.CENTER);

        setContentPane(contenido);
        pack();
        setLocationRelativeTo(propietario);

        animacion = new Timer(70, e -> spinner.avanzar());
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            animacion.start();
        } else {
            animacion.stop();
        }
        super.setVisible(visible);
    }

    /** Ocho puntos en círculo, cada uno más tenue según qué tan lejos esté del "frente" que gira. */
    private static class Spinner extends JComponent {
        private static final int PUNTOS = 8;
        private int paso = 0;

        void avanzar() {
            paso = (paso + 1) % PUNTOS;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int cx = w / 2;
            int cy = h / 2;
            int radio = Math.min(w, h) / 2 - 4;
            int tamanoPunto = Math.max(3, radio / 3);

            for (int i = 0; i < PUNTOS; i++) {
                double angulo = Math.PI * 2 * i / PUNTOS;
                int x = (int) (cx + Math.cos(angulo) * radio);
                int y = (int) (cy + Math.sin(angulo) * radio);
                int distancia = (i - paso + PUNTOS) % PUNTOS;
                int alpha = Math.max(40, 255 - distancia * 30);
                g2.setColor(new Color(33, 150, 243, alpha));
                g2.fillOval(x - tamanoPunto / 2, y - tamanoPunto / 2, tamanoPunto, tamanoPunto);
            }
            g2.dispose();
        }
    }
}
