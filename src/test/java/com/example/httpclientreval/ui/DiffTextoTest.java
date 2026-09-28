package com.example.httpclientreval.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiffTextoTest {

    @Test
    void textosIgualesNoTienenDiferencias() {
        DiffTexto.Resultado r = DiffTexto.calcular("a\nb\nc", "a\nb\nc");

        assertFalse(r.hayDiferencias());
        assertEquals(3, r.iguales);
        assertEquals(0, r.agregadas);
        assertEquals(0, r.eliminadas);
    }

    @Test
    void detectaLineasAgregadas() {
        DiffTexto.Resultado r = DiffTexto.calcular("a\nb", "a\nb\nc");

        assertTrue(r.hayDiferencias());
        assertEquals(2, r.iguales);
        assertEquals(1, r.agregadas);
        assertEquals(0, r.eliminadas);
        assertEquals(DiffTexto.Tipo.AGREGADA, r.lineas.get(r.lineas.size() - 1).tipo);
        assertEquals("c", r.lineas.get(r.lineas.size() - 1).texto);
    }

    @Test
    void detectaLineasEliminadas() {
        DiffTexto.Resultado r = DiffTexto.calcular("a\nb\nc", "a\nc");

        assertEquals(2, r.iguales);
        assertEquals(0, r.agregadas);
        assertEquals(1, r.eliminadas);
        assertEquals("b", r.lineas.get(1).texto);
        assertEquals(DiffTexto.Tipo.ELIMINADA, r.lineas.get(1).tipo);
    }

    @Test
    void unaLineaCambiadaSeVeComoEliminarYAgregar() {
        DiffTexto.Resultado r = DiffTexto.calcular("Codigo: 0", "Codigo: 99");

        assertEquals(0, r.iguales);
        assertEquals(1, r.agregadas);
        assertEquals(1, r.eliminadas);
    }

    @Test
    void ambosVacios() {
        DiffTexto.Resultado r = DiffTexto.calcular("", "");

        assertFalse(r.hayDiferencias());
        assertEquals(0, r.iguales);
        assertTrue(r.lineas.isEmpty());
    }

    @Test
    void unoVacioElOtroNo() {
        DiffTexto.Resultado r = DiffTexto.calcular("", "a\nb");

        assertEquals(2, r.agregadas);
        assertEquals(0, r.eliminadas);
    }

    @Test
    void nulosSeTratanComoVacios() {
        DiffTexto.Resultado r = DiffTexto.calcular(null, null);

        assertFalse(r.hayDiferencias());
        assertTrue(r.lineas.isEmpty());
    }

    @Test
    void textosDemasiadoGrandesNoSeComparan() {
        String grande = "x\n".repeat(3000);
        DiffTexto.Resultado r = DiffTexto.calcular(grande, grande + "y");

        assertTrue(r.demasiadoGrande);
        assertTrue(r.lineas.isEmpty());
    }
}
