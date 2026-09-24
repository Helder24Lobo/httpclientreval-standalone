package com.example.httpclientreval.ui;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.function.Supplier;

/**
 * Componente visual de recuadro/badge con fondo de color para notificaciones de
 * éxito, error e información. Reemplaza el texto plano monocromo sin marco por un
 * contenedor destacado, muy visible y visualmente atractivo.
 */
public class StatusBanner extends JPanel {

    private final JLabel iconoLabel = new JLabel();
    private final JLabel textoLabel = new JLabel();

    /** Cada tipo tiene su color e icono, para distinguirlos de un vistazo sin leer el texto. */
    public enum Tipo {
        EXITO(new Color(46, 125, 50), Icons::check),                  // Verde
        ERROR(new Color(198, 40, 40), Icons::alerta),                 // Rojo: error genérico
        ERROR_NEGOCIO(new Color(183, 80, 0), Icons::errorNegocio),    // Naranja: el servicio rechazó la operación
        ERROR_HTTP(new Color(136, 14, 79), Icons::errorHttp),         // Granate: falla a nivel de HTTP
        INFO(new Color(21, 101, 192), Icons::info);                   // Azul

        final Color colorFondo;
        final Supplier<Icon> icono;

        Tipo(Color colorFondo, Supplier<Icon> icono) {
            this.colorFondo = colorFondo;
            this.icono = icono;
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
        mostrar(Tipo.EXITO, mensaje);
    }

    /** Error genérico: validaciones, fallas de conexión o respuestas que no se pudieron interpretar. */
    public void mostrarError(String mensaje) {
        mostrar(Tipo.ERROR, mensaje);
    }

    /** La llamada llegó bien (HTTP 2xx) pero el servicio respondió con un código de negocio distinto de éxito. */
    public void mostrarErrorNegocio(String mensaje) {
        mostrar(Tipo.ERROR_NEGOCIO, mensaje);
    }

    /** El servidor respondió con un código HTTP fuera de 2xx. */
    public void mostrarErrorHttp(String mensaje) {
        mostrar(Tipo.ERROR_HTTP, mensaje);
    }

    public void mostrarInfo(String mensaje) {
        mostrar(Tipo.INFO, mensaje);
    }

    private void mostrar(Tipo tipo, String mensaje) {
        if (mensaje == null || mensaje.isBlank()) {
            ocultar();
            return;
        }
        this.tipoActual = tipo;
        iconoLabel.setIcon(tipo.icono.get());
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
