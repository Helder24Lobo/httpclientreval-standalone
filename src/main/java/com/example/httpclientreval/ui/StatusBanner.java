package com.example.httpclientreval.ui;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Componente visual de recuadro/badge con fondo de color para notificaciones de
 * éxito, error e información. Reemplaza el texto plano monocromo sin marco por un
 * contenedor destacado, muy visible y visualmente atractivo.
 */
public class StatusBanner extends JPanel {

    private final JLabel iconoLabel = new JLabel();
    private final JLabel textoLabel = new JLabel();

    public enum Tipo {
        EXITO(new Color(46, 125, 50)),   // Verde destacado
        ERROR(new Color(198, 40, 40)),   // Rojo destacado
        INFO(new Color(21, 101, 192));    // Azul destacado

        final Color colorFondo;

        Tipo(Color colorFondo) {
            this.colorFondo = colorFondo;
        }
    }

    private Tipo tipoActual = Tipo.INFO;

    public StatusBanner() {
        super(new FlowLayout(FlowLayout.LEFT, 8, 4));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));

        textoLabel.setForeground(Color.WHITE);
        textoLabel.setFont(textoLabel.getFont().deriveFont(Font.BOLD, 12f));

        add(iconoLabel);
        add(textoLabel);

        setVisible(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (isVisible() && textoLabel.getText() != null && !textoLabel.getText().isBlank()) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(tipoActual.colorFondo);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
            g2.dispose();
        }
        super.paintComponent(g);
    }

    public void mostrarExito(String mensaje) {
        if (mensaje == null || mensaje.isBlank()) {
            ocultar();
            return;
        }
        this.tipoActual = Tipo.EXITO;
        iconoLabel.setIcon(Icons.check());
        textoLabel.setText(mensaje);
        setVisible(true);
        notificarCambio();
    }

    public void mostrarError(String mensaje) {
        if (mensaje == null || mensaje.isBlank()) {
            ocultar();
            return;
        }
        this.tipoActual = Tipo.ERROR;
        iconoLabel.setIcon(Icons.alerta());
        textoLabel.setText(mensaje);
        setVisible(true);
        notificarCambio();
    }

    public void mostrarInfo(String mensaje) {
        if (mensaje == null || mensaje.isBlank()) {
            ocultar();
            return;
        }
        this.tipoActual = Tipo.INFO;
        iconoLabel.setIcon(Icons.info());
        textoLabel.setText(mensaje);
        setVisible(true);
        notificarCambio();
    }

    public void ocultar() {
        textoLabel.setText("");
        setVisible(false);
        notificarCambio();
    }

    private void notificarCambio() {
        revalidate();
        repaint();
        if (getParent() != null) {
            getParent().revalidate();
            getParent().repaint();
        }
    }
}
