package com.example.httpclientreval.ui;

import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ColorLegibleTest {

    // Colores de estado de FlatLaf (Actions.*) y fondos de tabla de los temas claro y oscuro (Darcula).
    private static final Color VERDE_CLARO = new Color(0x59A869);
    private static final Color ROJO_CLARO = new Color(0xDB5860);
    private static final Color AMARILLO_CLARO = new Color(0xEDA200);
    private static final Color VERDE_OSCURO = new Color(0x499C54);
    private static final Color ROJO_OSCURO = new Color(0xC75450);
    private static final Color AMARILLO_OSCURO = new Color(0xF0A732);
    private static final Color FONDO_CLARO = Color.WHITE;
    private static final Color FONDO_OSCURO = new Color(0x45494A);

    @Test
    void contrasteWcagDeReferencia() {
        assertEquals(21.0, ColorLegible.contraste(Color.BLACK, Color.WHITE), 0.01);
        assertEquals(1.0, ColorLegible.contraste(FONDO_OSCURO, FONDO_OSCURO), 0.01);
    }

    @Test
    void enTemaClaroLosColoresSeOscurecenHastaLeerse() {
        for (Color base : new Color[]{VERDE_CLARO, ROJO_CLARO, AMARILLO_CLARO}) {
            Color ajustado = ColorLegible.sobre(base, FONDO_CLARO);
            assertTrue(ColorLegible.contraste(ajustado, FONDO_CLARO) >= ColorLegible.CONTRASTE_MINIMO);
            assertTrue(suma(ajustado) <= suma(base), "debe oscurecer, no aclarar");
        }
    }

    @Test
    void enTemaOscuroLosColoresSeAclaranHastaLeerse() {
        for (Color base : new Color[]{VERDE_OSCURO, ROJO_OSCURO, AMARILLO_OSCURO}) {
            Color ajustado = ColorLegible.sobre(base, FONDO_OSCURO);
            assertTrue(ColorLegible.contraste(ajustado, FONDO_OSCURO) >= ColorLegible.CONTRASTE_MINIMO);
            assertTrue(suma(ajustado) >= suma(base), "debe aclarar, no oscurecer");
        }
    }

    @Test
    void conservaElMatiz() {
        Color verde = ColorLegible.sobre(VERDE_OSCURO, FONDO_OSCURO);
        assertTrue(verde.getGreen() > verde.getRed() && verde.getGreen() > verde.getBlue());
        Color rojo = ColorLegible.sobre(ROJO_CLARO, FONDO_CLARO);
        assertTrue(rojo.getRed() > rojo.getGreen() && rojo.getRed() > rojo.getBlue());
    }

    @Test
    void unColorQueYaContrastaNoSeToca() {
        Color oscuro = new Color(20, 90, 30);
        assertEquals(oscuro, ColorLegible.sobre(oscuro, FONDO_CLARO));
        assertEquals(oscuro, ColorLegible.sobre(oscuro, null));
    }

    private static int suma(Color c) {
        return c.getRed() + c.getGreen() + c.getBlue();
    }
}
