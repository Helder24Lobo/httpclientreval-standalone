package com.example.httpclientreval.ui;

import com.example.httpclientreval.model.EnvioRegistrado.Resultado;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BarraEstadoTest {

    private static final LocalDateTime HORA = LocalDateTime.of(2026, 10, 8, 15, 10, 7);

    @Test
    void totalDeTransacciones() {
        assertEquals("Total de transacciones: 15", BarraEstado.textoTotal(15));
        assertEquals("Total de transacciones: 0", BarraEstado.textoTotal(0));
    }

    @Test
    void ultimoEnvioConEstadoYHora() {
        assertEquals("Último envío: Exitoso - 15:10:07", BarraEstado.textoUltimoEnvio(Resultado.EXITO, HORA));
        assertEquals("Último envío: Error HTTP - 15:10:07", BarraEstado.textoUltimoEnvio(Resultado.ERROR_HTTP, HORA));
    }

    @Test
    void cadaResultadoTieneSuEstado() {
        assertEquals("Exitoso", BarraEstado.estado(Resultado.EXITO));
        assertEquals("Error de negocio", BarraEstado.estado(Resultado.ERROR_NEGOCIO));
        assertEquals("Error HTTP", BarraEstado.estado(Resultado.ERROR_HTTP));
        assertEquals("Respuesta incompleta", BarraEstado.estado(Resultado.INCOMPLETO));
        assertEquals("Sin respuesta", BarraEstado.estado(Resultado.SIN_RESPUESTA));
    }

    @Test
    void sinEnviosEnLaSesion() {
        assertEquals("Último envío: ninguno en esta sesión", BarraEstado.textoSinEnvios());
    }
}
