package com.example.httpclientreval.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormatoSalidaTest {

    @Test
    void jsonSeIndentaYNoEscapaCaracteres() {
        FormatoSalida.Resultado r = FormatoSalida.formatear("{\"a\":\"x<y&z\",\"b\":null,\"c\":[1,2]}");
        assertEquals(FormatoSalida.Tipo.JSON, r.tipo);
        assertTrue(r.texto.contains("\n  \"a\": \"x<y&z\""), r.texto);
        assertTrue(r.texto.contains("\"b\": null"), r.texto);
    }

    @Test
    void xmlSeIndentaYRespetaLaDeclaracion() {
        FormatoSalida.Resultado r = FormatoSalida.formatear(
                "<?xml version=\"1.0\"?><s:Envelope xmlns:s=\"urn:x\"><s:Body><a>1</a></s:Body></s:Envelope>");
        assertEquals(FormatoSalida.Tipo.XML, r.tipo);
        assertTrue(r.texto.contains("\n  <s:Body>\n    <a>1</a>\n  </s:Body>"), r.texto);
    }

    @Test
    void xmlYaIndentadoNoDuplicaLineasEnBlanco() {
        FormatoSalida.Resultado r = FormatoSalida.formatear("<a>\n  <b>1</b>\n</a>");
        assertEquals("<a>\n  <b>1</b>\n</a>", r.texto);
    }

    @Test
    void xmlConDoctypeSeRechazaYQuedaComoTexto() {
        String malicioso = "<!DOCTYPE a [<!ENTITY e SYSTEM \"file:///etc/passwd\">]><a>&e;</a>";
        FormatoSalida.Resultado r = FormatoSalida.formatear(malicioso);
        assertEquals(FormatoSalida.Tipo.TEXTO, r.tipo);
        assertEquals(malicioso, r.texto);
    }

    @Test
    void textoInvalidoOBase64QuedaIgual() {
        assertEquals(FormatoSalida.Tipo.TEXTO, FormatoSalida.formatear("{roto").tipo);
        assertEquals("{roto", FormatoSalida.formatear("{roto").texto);
        assertEquals(FormatoSalida.Tipo.TEXTO, FormatoSalida.formatear("QUJDREVGRw==").tipo);
        assertEquals("", FormatoSalida.formatear(null).texto);
    }
}
