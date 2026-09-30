package com.example.httpclientreval.ui;

import com.example.httpclientreval.model.SoapHttpClient;
import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Rótulo PERMANENTE (a diferencia de {@link StatusBanner}, nunca se oculta) con el ambiente al que
 * apunta {@link SoapHttpClient}: Pruebas, Producción o Personalizado. Va siempre visible en la
 * ventana principal para que cambiar la URL en Configuración no pase inadvertido — en particular,
 * para no terminar enviando a producción sin darse cuenta.
 */
final class AmbienteBanner extends JPanel {

    private enum Estilo {
        PRUEBAS("PRUEBAS", new Color(46, 125, 50)),           // Verde: el ambiente de siempre, sin sorpresas
        PRODUCCION("⚠ PRODUCCIÓN", new Color(198, 40, 40)),   // Rojo: confirmado a propósito, máxima alerta
        PERSONALIZADO("AMBIENTE PERSONALIZADO", new Color(183, 80, 0)); // Naranja: no es pruebas ni se confirmó como producción

        final String texto;
        final Color colorFondo;

        Estilo(String texto, Color colorFondo) {
            this.texto = texto;
            this.colorFondo = colorFondo;
        }
    }

    private final JLabel etiqueta = new JLabel();

    AmbienteBanner() {
        super(new FlowLayout(FlowLayout.LEFT, 0, 0));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));

        etiqueta.setForeground(Color.WHITE);
        etiqueta.putClientProperty(FlatClientProperties.STYLE, "font: bold -1");
        add(etiqueta);

        actualizar();
    }

    /** Vuelve a leer {@link SoapHttpClient} (llamar después de cualquier cambio hecho en Configuración). */
    void actualizar() {
        Estilo estilo = switch (SoapHttpClient.getAmbiente()) {
            case PRUEBAS -> Estilo.PRUEBAS;
            case PRODUCCION -> Estilo.PRODUCCION;
            case PERSONALIZADO -> Estilo.PERSONALIZADO;
        };
        etiqueta.setText(estilo.texto);
        setToolTipText("Los envíos van a: " + SoapHttpClient.getUrl());
        Accesibilidad.nombrar(this, "Ambiente activo: " + estilo.texto, "Los envíos van a " + SoapHttpClient.getUrl());
        putClientProperty("ambiente.color", estilo.colorFondo);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Object color = getClientProperty("ambiente.color");
        if (color instanceof Color fondo) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(fondo);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
            g2.dispose();
        }
        super.paintComponent(g);
    }
}
