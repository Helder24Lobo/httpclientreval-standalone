package com.example.httpclientreval.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FuentePreferenciasTest {

    @Test
    void desplazarSubeYBajaUnEscalon() {
        assertEquals(115, FuentePreferencias.desplazar(100, 1));
        assertEquals(90, FuentePreferencias.desplazar(100, -1));
        assertEquals(150, FuentePreferencias.desplazar(130, 1));
    }

    @Test
    void desplazarNoSeSaleDeLosExtremos() {
        int maximo = FuentePreferencias.PORCENTAJES[FuentePreferencias.PORCENTAJES.length - 1];
        int minimo = FuentePreferencias.PORCENTAJES[0];
        assertEquals(maximo, FuentePreferencias.desplazar(maximo, 1));
        assertEquals(minimo, FuentePreferencias.desplazar(minimo, -1));
    }

    @Test
    void unValorFueraDeLaListaArrancaDesdeElCienPorCiento() {
        assertEquals(115, FuentePreferencias.desplazar(137, 1));
        assertEquals(90, FuentePreferencias.desplazar(-5, -1));
    }
}
