package com.example.httpclientreval.ui;

import com.example.httpclientreval.model.EnvioRegistrado;

import javax.swing.UIManager;
import java.awt.Color;

/**
 * Colores de texto que se leen bien sobre cualquier fondo, claro u oscuro. Parte del color del tema
 * (las claves de FlatLaf cambian solas entre el tema claro y el oscuro) y, si no contrasta lo
 * suficiente con el fondo, lo aclara u oscurece lo justo: en el oscuro quedan verdes/rojos suaves y
 * en el claro tonos más profundos, sin perder el matiz.
 */
final class ColorLegible {

    /** Contraste mínimo WCAG AA para texto normal. */
    static final double CONTRASTE_MINIMO = 4.5;

    private static final double PASO = 0.05;

    private ColorLegible() {
    }

    /** Color de cómo terminó un envío (verde éxito, naranja error de negocio, rojo el resto), legible sobre {@code fondo}. */
    static Color deResultado(EnvioRegistrado.Resultado resultado, Color fondo) {
        switch (resultado) {
            case EXITO:
                return delTema("Actions.Green", new Color(56, 158, 66), fondo);
            case ERROR_NEGOCIO:
                return delTema("Actions.Yellow", new Color(230, 120, 0), fondo);
            default:
                return delTema("Actions.Red", new Color(220, 60, 60), fondo);
        }
    }

    /** Color de {@code claveTema} (o {@code respaldo} si el look and feel no la define) ajustado para leerse sobre {@code fondo}. */
    static Color delTema(String claveTema, Color respaldo, Color fondo) {
        Color base = UIManager.getColor(claveTema);
        return sobre(base != null ? base : respaldo, fondo);
    }

    /** {@code base} tal cual si ya contrasta con {@code fondo}; si no, mezclado hacia blanco o negro hasta lograrlo. */
    static Color sobre(Color base, Color fondo) {
        if (fondo == null || contraste(base, fondo) >= CONTRASTE_MINIMO) {
            return base;
        }
        Color extremo = contraste(Color.WHITE, fondo) >= contraste(Color.BLACK, fondo) ? Color.WHITE : Color.BLACK;
        Color resultado = base;
        for (double t = PASO; t <= 1.0 + 1e-9; t += PASO) {
            resultado = mezclar(base, extremo, t);
            if (contraste(resultado, fondo) >= CONTRASTE_MINIMO) {
                return resultado;
            }
        }
        return resultado;
    }

    /** Razón de contraste WCAG entre dos colores (de 1 a 21). */
    static double contraste(Color a, Color b) {
        double la = luminancia(a);
        double lb = luminancia(b);
        return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
    }

    private static double luminancia(Color c) {
        return 0.2126 * lineal(c.getRed()) + 0.7152 * lineal(c.getGreen()) + 0.0722 * lineal(c.getBlue());
    }

    private static double lineal(int canal) {
        double s = canal / 255.0;
        return s <= 0.03928 ? s / 12.92 : Math.pow((s + 0.055) / 1.055, 2.4);
    }

    private static Color mezclar(Color a, Color b, double t) {
        return new Color(
                (int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
                (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }
}
