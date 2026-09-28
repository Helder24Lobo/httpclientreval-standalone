package com.example.httpclientreval.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SoapHttpClientCurlTest {

    @Test
    void trael_metodo_la_url_y_las_cabeceras_fijas() {
        String curl = SoapHttpClient.curlPara("<a>1</a>");

        assertTrue(curl.startsWith("curl -X POST '"));
        assertTrue(curl.contains("RVLRRT.svc"));
        assertTrue(curl.contains("-H 'Content-Type: text/xml; charset=utf-8'"));
        assertTrue(curl.contains("-H 'SOAPAction: http://tempuri.org/IRVLRRT/OBJRequest'"));
        assertTrue(curl.contains("--data-raw '<a>1</a>'"));
    }

    @Test
    void escapaComillasSimplesEnElCuerpo() {
        String curl = SoapHttpClient.curlPara("<a valor='x'>y</a>");

        // La comilla se cierra, se inserta una comilla escapada y se reabre: '\''
        assertTrue(curl.contains("valor='\\''x'\\''"));
    }

    @Test
    void cuerpoNuloNoRompeElComando() {
        assertTrue(SoapHttpClient.curlPara(null).endsWith("--data-raw ''"));
    }
}
