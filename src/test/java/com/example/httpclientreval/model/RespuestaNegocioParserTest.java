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
}
