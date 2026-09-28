package com.example.httpclientreval.ui;

import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.FlowLayout;
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
        EXITO("Éxito", new Color(46, 125, 50), Icons::check),                  // Verde
        ERROR("Error", new Color(198, 40, 40), Icons::alerta),                 // Rojo: error genérico
        ERROR_NEGOCIO("Error de negocio", new Color(183, 80, 0), Icons::errorNegocio), // Naranja: el servicio rechazó la operación
        ERROR_HTTP("Error HTTP", new Color(136, 14, 79), Icons::errorHttp),    // Granate: falla a nivel de HTTP
        INFO("Información", new Color(21, 101, 192), Icons::info);             // Azul

        /** Lo que lee un lector de pantalla en lugar del icono y el color, que por sí solos no se perciben. */
        final String etiqueta;
        final Color colorFondo;
        final Supplier<Icon> icono;

        Tipo(String etiqueta, Color colorFondo, Supplier<Icon> icono) {
            this.etiqueta = etiqueta;
            this.colorFondo = colorFondo;
            this.icono = icono;
        }
    }

    private Tipo tipoActual = Tipo.INFO;

    public StatusBanner() {
        super(new FlowLayout(FlowLayout.LEFT, 8, 4));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        Accesibilidad.nombrar(this, "Estado");

        textoLabel.setForeground(Color.WHITE);
        textoLabel.putClientProperty(FlatClientProperties.STYLE, "font: bold");

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
        // El tipo va en el nombre del banner: el cambio de nombre se notifica a los lectores de pantalla.
        Accesibilidad.nombrar(iconoLabel, tipo.etiqueta);
        Accesibilidad.nombrar(this, tipo.etiqueta + ": " + mensaje);
        setVisible(true);
        notificarCambio();
    }

    public void ocultar() {
        Accesibilidad.nombrar(this, "Estado");
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
