package com.example.httpclientreval.ui;

import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.util.function.BiConsumer;

/**
 * Iconos vectoriales simples (16x16) con color real, dibujados a mano con
 * Graphics2D. Java/Swing no renderiza emoji a color de forma confiable en
 * botones/pestañas (suelen salir como glifos monocromos), así que en vez de
 * depender de eso, se dibujan formas propias.
 */
final class Icons {

    private Icons() {
    }

    private static Icon icono(int size, Color color, BiConsumer<Graphics2D, Integer> dibujo) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.translate(x, y);
                g2.setColor(color);
                dibujo.accept(g2, size);
                g2.dispose();
            }

            @Override
            public int getIconWidth() {
                return size;
            }

            @Override
            public int getIconHeight() {
                return size;
            }
        };
    }

    static Icon generar() {
        return icono(16, new Color(255, 193, 7), (g, s) -> {
            Path2D rayo = new Path2D.Double();
            rayo.moveTo(s * 0.55, 0);
            rayo.lineTo(s * 0.15, s * 0.58);
            rayo.lineTo(s * 0.45, s * 0.58);
            rayo.lineTo(s * 0.35, s);
            rayo.lineTo(s * 0.85, s * 0.4);
            rayo.lineTo(s * 0.5, s * 0.4);
            rayo.closePath();
            g.fill(rayo);
        });
    }

    static Icon enviar() {
        return icono(16, new Color(33, 150, 243), (g, s) -> {
            Path2D flecha = new Path2D.Double();
            flecha.moveTo(s * 0.2, s * 0.1);
            flecha.lineTo(s * 0.9, s * 0.5);
            flecha.lineTo(s * 0.2, s * 0.9);
            flecha.closePath();
            g.fill(flecha);
        });
    }

    static Icon limpiar() {
        return icono(16, new Color(244, 67, 54), (g, s) -> {
            g.fillRoundRect((int) (s * 0.25), (int) (s * 0.35), (int) (s * 0.5), (int) (s * 0.55), 2, 2);
            g.fillRect((int) (s * 0.15), (int) (s * 0.2), (int) (s * 0.7), (int) (s * 0.1));
            g.fillRect((int) (s * 0.4), (int) (s * 0.05), (int) (s * 0.2), (int) (s * 0.15));
        });
    }

    static Icon guardar() {
        return icono(16, new Color(76, 175, 80), (g, s) -> {
            g.fillRoundRect(0, 0, s, s, 3, 3);
            g.setColor(Color.WHITE);
            g.fillRect((int) (s * 0.25), (int) (s * 0.05), (int) (s * 0.5), (int) (s * 0.35));
            g.fillRect((int) (s * 0.2), (int) (s * 0.55), (int) (s * 0.6), (int) (s * 0.4));
        });
    }

    static Icon descifrar() {
        return icono(16, new Color(255, 152, 0), (g, s) -> {
            g.fillRoundRect((int) (s * 0.2), (int) (s * 0.45), (int) (s * 0.6), (int) (s * 0.5), 3, 3);
            g.setStroke(new BasicStroke(2f));
            g.drawArc((int) (s * 0.25), (int) (s * 0.05), (int) (s * 0.5), (int) (s * 0.5), 0, 180);
        });
    }

    static Icon nuevo() {
        return icono(16, new Color(76, 175, 80), (g, s) -> {
            g.fillRect((int) (s * 0.4), (int) (s * 0.1), (int) (s * 0.2), (int) (s * 0.8));
            g.fillRect((int) (s * 0.1), (int) (s * 0.4), (int) (s * 0.8), (int) (s * 0.2));
        });
    }

    static Icon copiar() {
        return icono(16, new Color(3, 169, 244), (g, s) -> {
            g.setStroke(new BasicStroke(1.5f));
            g.drawRoundRect((int) (s * 0.15), (int) (s * 0.3), (int) (s * 0.55), (int) (s * 0.6), 2, 2);
            g.drawRoundRect((int) (s * 0.3), (int) (s * 0.1), (int) (s * 0.55), (int) (s * 0.6), 2, 2);
        });
    }

    static Icon editar() {
        return icono(16, new Color(171, 71, 188), (g, s) -> {
            Path2D lapiz = new Path2D.Double();
            lapiz.moveTo(s * 0.1, s * 0.9);
            lapiz.lineTo(s * 0.15, s * 0.6);
            lapiz.lineTo(s * 0.7, s * 0.05);
            lapiz.lineTo(s * 0.95, s * 0.3);
            lapiz.lineTo(s * 0.4, s * 0.85);
            lapiz.closePath();
            g.fill(lapiz);
        });
    }

    static Icon configuracion() {
        return icono(16, new Color(158, 158, 158), (g, s) -> {
            g.setStroke(new BasicStroke(2f));
            g.drawLine((int) (s * 0.1), (int) (s * 0.3), (int) (s * 0.9), (int) (s * 0.3));
            g.drawLine((int) (s * 0.1), (int) (s * 0.7), (int) (s * 0.9), (int) (s * 0.7));
            g.fillOval((int) (s * 0.55), (int) (s * 0.22), (int) (s * 0.16), (int) (s * 0.16));
            g.fillOval((int) (s * 0.3), (int) (s * 0.62), (int) (s * 0.16), (int) (s * 0.16));
        });
    }
}
