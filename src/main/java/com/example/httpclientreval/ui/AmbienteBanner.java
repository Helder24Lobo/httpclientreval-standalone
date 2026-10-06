package com.example.httpclientreval.ui;

import com.example.httpclientreval.model.Entorno;
import com.example.httpclientreval.model.Entornos;
import com.example.httpclientreval.model.SoapHttpClient;
import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Rótulo PERMANENTE (a diferencia de {@link StatusBanner}, nunca se oculta) con el ambiente activo:
 * verde para el de Pruebas, rojo para uno marcado como Producción y naranja para cualquier otro
 * (Desarrollo, QA...). Va siempre visible en la ventana principal para que cambiar de ambiente no pase
 * inadvertido — en particular, para no enviar a producción sin darse cuenta. Al pulsarlo se abre el
 * administrador de ambientes.
 */
final class AmbienteBanner extends JPanel {

    private static final Color VERDE = new Color(46, 125, 50);
    private static final Color ROJO = new Color(198, 40, 40);
    private static final Color NARANJA = new Color(183, 80, 0);

    private final JLabel etiqueta = new JLabel();
    private Color fondo = VERDE;
    private Runnable alPulsar;

    AmbienteBanner() {
        super(new FlowLayout(FlowLayout.LEFT, 0, 0));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));

        etiqueta.setForeground(Color.WHITE);
        etiqueta.putClientProperty(FlatClientProperties.STYLE, "font: bold -1");
        add(etiqueta);

        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (alPulsar != null) {
                    alPulsar.run();
                }
            }
        });

        actualizar();
    }

    /** Qué hacer al pulsar el rótulo (abrir el administrador de ambientes). */
    void alPulsar(Runnable accion) {
        this.alPulsar = accion;
    }

    /** Vuelve a leer el ambiente activo (llamar después de cualquier cambio hecho en el administrador). */
    void actualizar() {
        Entorno activo = Entornos.instancia().activo();
        String texto;
        switch (SoapHttpClient.ambienteDe(activo)) {
            case PRODUCCION:
                fondo = ROJO;
                texto = activo.nombre.equalsIgnoreCase(Entornos.NOMBRE_PRODUCCION)
                        ? "⚠ PRODUCCIÓN" : "⚠ PRODUCCIÓN: " + activo.nombre;
                break;
            case PRUEBAS:
                fondo = VERDE;
                texto = activo.nombre.equalsIgnoreCase(Entornos.NOMBRE_PRUEBAS) ? "PRUEBAS" : "PRUEBAS: " + activo.nombre;
                break;
            default:
                fondo = NARANJA;
                texto = "AMBIENTE: " + activo.nombre;
        }
        etiqueta.setText(texto);
        setToolTipText("Los envíos van a: " + activo.url + " — clic para cambiar de ambiente");
        Accesibilidad.nombrar(this, "Ambiente activo: " + texto, "Los envíos van a " + activo.url
                + ". Pulsa para abrir el administrador de ambientes");
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(fondo);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
        g2.dispose();
        super.paintComponent(g);
    }
}
