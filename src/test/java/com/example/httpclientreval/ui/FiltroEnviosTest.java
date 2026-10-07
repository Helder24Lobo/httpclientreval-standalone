package com.example.httpclientreval.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FiltroEnviosTest {

    private static final String ENVIO =
            "10:30:05 Recaudos - Consulta Pruebas Código 0 200 250 ms Transacción exitosa";

    @Test
    void consultaVaciaCoincideConTodo() {
        assertTrue(FiltroEnvios.coincide(ENVIO, ""));
        assertTrue(FiltroEnvios.coincide(ENVIO, "   "));
        assertTrue(FiltroEnvios.coincide(ENVIO, null));
    }

    @Test
    void ignoraMayusculasYTildes() {
        assertTrue(FiltroEnvios.coincide(ENVIO, "RECAUDOS"));
        assertTrue(FiltroEnvios.coincide(ENVIO, "codigo"));
        assertTrue(FiltroEnvios.coincide(ENVIO, "transaccion EXITOSA"));
        assertTrue(FiltroEnvios.coincide("Código", "codigo"));
        assertTrue(FiltroEnvios.coincide("Codigo", "código"));
    }

    @Test
    void todasLasPalabrasDebenAparecerEnCualquierOrden() {
        assertTrue(FiltroEnvios.coincide(ENVIO, "consulta recaudos"));
        assertTrue(FiltroEnvios.coincide(ENVIO, "  recaudos   10:30 "));
        assertFalse(FiltroEnvios.coincide(ENVIO, "recaudos retiro"));
    }

    @Test
    void sinCoincidenciaDevuelveFalso() {
        assertFalse(FiltroEnvios.coincide(ENVIO, "consignaciones"));
        assertFalse(FiltroEnvios.coincide(null, "algo"));
        assertFalse(FiltroEnvios.coincide(null, "algo", FiltroEnvios.Estado.TODOS));
    }
}
