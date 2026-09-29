package com.example.httpclientreval.ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Ventana modal chica con un ícono de carga animado, para mostrar mientras se espera la respuesta de
 * una operación larga (ej. el envío al WS), con un botón "Cancelar" (también con Esc o la X de la
 * ventana) que interrumpe esa espera. Uso: crearla, arrancar la tarea en segundo plano, y en su
 * callback de finalización llamar dispose() — eso desbloquea el setVisible(true) modal.
 */
public class EnviandoDialog extends JDialog {

    private final Timer animacion;
    private final JButton cancelar;
    private boolean cancelando = false;

    /** {@code alCancelar} se invoca una sola vez (botón, Esc o la X) y solo antes de que el llamador cierre el diálogo. */
    public EnviandoDialog(Window propietario, String mensaje, Runnable alCancelar) {
        super(propietario, "Enviando", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        setResizable(false);

        Spinner spinner = new Spinner();
        spinner.setPreferredSize(new Dimension(36, 36));

        cancelar = new JButton("Cancelar");
        Accesibilidad.nombrar(cancelar, "Cancelar envío");
        cancelar.addActionListener(e -> cancelar(alCancelar));

        JPanel sur = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        sur.add(cancelar);

        JPanel contenido = new JPanel(new BorderLayout(16, 4));
        contenido.setBorder(BorderFactory.createEmptyBorder(24, 28, 16, 28));
        contenido.add(spinner, BorderLayout.WEST);
        contenido.add(new JLabel(mensaje), BorderLayout.CENTER);
        contenido.add(sur, BorderLayout.SOUTH);

        setContentPane(contenido);

        // DO_NOTHING_ON_CLOSE deja la X sin efecto por defecto; se conecta a la misma cancelación.
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cancelar(alCancelar);
            }
        });
        getRootPane().registerKeyboardAction(e -> cancelar(alCancelar),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        pack();
        setLocationRelativeTo(propietario);

        animacion = new Timer(70, e -> spinner.avanzar());
    }

    private void cancelar(Runnable alCancelar) {
        // La cancelación puede tardar un poco en resolverse (el hilo de fondo debe notar la interrupción);
        // se deshabilita el botón para no lanzarla dos veces y se avisa que ya se está procesando.
        if (cancelando) {
            return;
        }
        cancelando = true;
        cancelar.setEnabled(false);
        cancelar.setText("Cancelando…");
        alCancelar.run();
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
