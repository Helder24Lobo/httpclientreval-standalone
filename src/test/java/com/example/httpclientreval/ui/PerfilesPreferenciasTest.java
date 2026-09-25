package com.example.httpclientreval.ui;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PerfilesPreferenciasTest {

    private Preferences nodo;
    private PerfilesPreferencias prefs;

    @BeforeEach
    void abrir() {
        // Nodo propio para no tocar los favoritos reales del usuario.
        nodo = Preferences.userRoot().node("httpclientreval-test-" + System.nanoTime());
        prefs = new PerfilesPreferencias(nodo);
    }

    @AfterEach
    void cerrar() throws BackingStoreException {
        nodo.removeNode();
    }

    @Test
    void alternarFavoritoMarcaYDesmarca() {
        assertTrue(prefs.alternarFavorito("A - uno"));
        assertTrue(prefs.esFavorito("A - uno"));
        assertFalse(prefs.alternarFavorito("A - uno"));
        assertFalse(prefs.esFavorito("A - uno"));
        assertTrue(prefs.favoritos().isEmpty());
    }

    @Test
    void favoritosConservanElOrdenDeMarcado() {
        prefs.alternarFavorito("B");
        prefs.alternarFavorito("A");
        assertEquals(List.of("B", "A"), prefs.favoritos());
    }

    @Test
    void recientesVanDelMasNuevoAlMasAntiguoSinRepetirYConLimite() {
        for (int i = 1; i <= PerfilesPreferencias.MAX_RECIENTES + 3; i++) {
            prefs.registrarReciente("P" + i);
        }
        prefs.registrarReciente("P5");

        List<String> recientes = prefs.recientes();
        assertEquals(PerfilesPreferencias.MAX_RECIENTES, recientes.size());
        assertEquals("P5", recientes.get(0));
        assertEquals(1, recientes.stream().filter("P5"::equals).count());
        assertFalse(recientes.contains("P1"));
    }

    @Test
    void renombrarConservaFavoritoYReciente() {
        prefs.alternarFavorito("Viejo");
        prefs.registrarReciente("Otro");
        prefs.registrarReciente("Viejo");

        prefs.renombrar("Viejo", "Nuevo");

        assertEquals(List.of("Nuevo"), prefs.favoritos());
        assertEquals(List.of("Nuevo", "Otro"), prefs.recientes());
    }

    @Test
    void eliminarOlvidaElPerfilEnAmbasListas() {
        prefs.alternarFavorito("X");
        prefs.registrarReciente("X");
        prefs.registrarReciente("Y");

        prefs.eliminar("X");

        assertTrue(prefs.favoritos().isEmpty());
        assertEquals(List.of("Y"), prefs.recientes());
    }
}
