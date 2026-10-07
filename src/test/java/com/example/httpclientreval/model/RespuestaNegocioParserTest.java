package com.example.httpclientreval.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RespuestaNegocioParserTest {

    @Test
    void extraerCodigo_leeElCodigoDelHeader() {
        String json = "{\"header\":{\"Codigo\":91,\"Mensaje\":\"WS: SU BANCO NO RESPONDE\"},\"Data\":{}}";

        assertEquals(91, RespuestaNegocioParser.extraerCodigo(json));
    }

    @Test
    void extraerCodigo_cero_esExito() {
        String json = "{\"header\":{\"Codigo\":0,\"Mensaje\":\"OK\"},\"Data\":{}}";

        assertEquals(0, RespuestaNegocioParser.extraerCodigo(json));
    }

    @Test
    void extraerCodigo_devuelveNullSiNoHayHeader() {
        assertNull(RespuestaNegocioParser.extraerCodigo("{\"foo\":\"bar\"}"));
    }

    @Test
    void extraerCodigo_devuelveNullSiJsonInvalido() {
        assertNull(RespuestaNegocioParser.extraerCodigo("no es json"));
    }

    @Test
    void extraerCodigo_devuelveNullSiJsonNullOVacio() {
        assertNull(RespuestaNegocioParser.extraerCodigo(null));
        assertNull(RespuestaNegocioParser.extraerCodigo(""));
        assertNull(RespuestaNegocioParser.extraerCodigo("   "));
    }

    @Test
    void extraerCodigo_devuelveNullSiJsonEsArray() {
        assertNull(RespuestaNegocioParser.extraerCodigo("[1, 2, 3]"));
    }

    @Test
    void extraerCodigo_toleraCodigoComoCadena() {
        assertEquals(42, RespuestaNegocioParser.extraerCodigo("{\"header\":{\"Codigo\":\"42\",\"Mensaje\":\"OK\"}}"));
    }
}
