package com.example.httpclientreval.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SoapRequestBuilderTest {

    @Test
    void build_incluyeCredencialesYElSobreDentroDeTemData() {
        String sobreJson = "{\"_header\":{\"IdTransaccion\":1,\"Idcliente\":26,\"IpCliente\":\"172.17.0.4\"},"
                + "\"_body\":{\"_mensaje\":\"ABC123==\"}}";

        String xml = SoapRequestBuilder.build("rrtpruebas", "ServicioPRB01*", sobreJson);

        assertTrue(xml.contains("<wsse:Username>rrtpruebas</wsse:Username>"));
        assertTrue(xml.contains("<wsse:Password>ServicioPRB01*</wsse:Password>"));
        assertTrue(xml.contains("<tem:data>" + sobreJson + "</tem:data>"));
        assertTrue(xml.contains("xmlns:tem=\"http://tempuri.org/\""));
    }

    @Test
    void build_escapaCaracteresXmlEnElSobre() {
        String sobreConAmpersand = "{\"Observacion\":\"A & B < C\"}";

        String xml = SoapRequestBuilder.build("user", "pass", sobreConAmpersand);

        assertTrue(xml.contains("A &amp; B &lt; C"));
    }
}
