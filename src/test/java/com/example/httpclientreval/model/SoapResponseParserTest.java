package com.example.httpclientreval.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SoapResponseParserTest {

    @Test
    void extraerObjRequestResult_sinPrefijoDeNamespace() {
        String xml = "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                + "<s:Body>"
                + "<OBJRequestResponse xmlns=\"http://tempuri.org/\">"
                + "<OBJRequestResult>ABC123==</OBJRequestResult>"
                + "</OBJRequestResponse>"
                + "</s:Body>"
                + "</s:Envelope>";

        assertEquals("ABC123==", SoapResponseParser.extraerObjRequestResult(xml));
    }

    @Test
    void extraerObjRequestResult_conPrefijoDeNamespace() {
        String xml = "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:a=\"http://tempuri.org/\">"
                + "<s:Body>"
                + "<a:OBJRequestResponse>"
                + "<a:OBJRequestResult>XYZ789==</a:OBJRequestResult>"
                + "</a:OBJRequestResponse>"
                + "</s:Body>"
                + "</s:Envelope>";

        assertEquals("XYZ789==", SoapResponseParser.extraerObjRequestResult(xml));
    }

    @Test
    void extraerObjRequestResult_devuelveNullSiNoAparece() {
        String xml = "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                + "<s:Body><s:Fault><faultstring>Error</faultstring></s:Fault></s:Body>"
                + "</s:Envelope>";

        assertNull(SoapResponseParser.extraerObjRequestResult(xml));
    }

    @Test
    void extraerObjRequestResult_devuelveNullSiXmlInvalido() {
        assertNull(SoapResponseParser.extraerObjRequestResult("esto no es XML"));
    }
}
