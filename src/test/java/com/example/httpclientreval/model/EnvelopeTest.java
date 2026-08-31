package com.example.httpclientreval.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EnvelopeTest {

    @Test
    void buildYParse_recuperaLosMismosDatos() {
        String sobreJson = Envelope.build("MENSAJE_CIFRADO_BASE64", 26, 1, "172.17.0.4");

        Envelope.Sobre sobre = Envelope.parse(sobreJson);

        assertEquals("MENSAJE_CIFRADO_BASE64", sobre.body.mensaje);
        assertEquals(26, sobre.header.idCliente);
        assertEquals(1, sobre.header.idTransaccion);
        assertEquals("172.17.0.4", sobre.header.ipCliente);
    }

    @Test
    void extraerMensaje_desdeSobreCompleto() {
        String sobreJson = "{\"_header\":{\"IdTransaccion\":1,\"Idcliente\":26,\"IpCliente\":\"172.17.0.4\"},"
                + "\"_body\":{\"_mensaje\":\"ABC123==\"}}";

        assertEquals("ABC123==", Envelope.extraerMensaje(sobreJson));
    }
}
