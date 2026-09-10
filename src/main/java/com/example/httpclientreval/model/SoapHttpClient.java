package com.example.httpclientreval.model;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Envía el XML SOAP armado por SoapRequestBuilder directo al webservice
 * RVLRRT, para no depender de copiar/pegar a Postman. URL, SOAPAction y
 * Content-Type son fijos para todo el servicio (no cambian por transacción).
 */
public class SoapHttpClient {

    private static final String URL = "https://servicios.reval.co:8110/RVLRRTpruebas/RVLRRT.svc";
    private static final String SOAP_ACTION = "http://tempuri.org/IRVLRRT/OBJRequest";
    private static final String CONTENT_TYPE = "text/xml; charset=utf-8";
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private SoapHttpClient() {
    }

    public static class Respuesta {
        public final int statusCode;
        public final String cuerpo;

        public Respuesta(int statusCode, String cuerpo) {
            this.statusCode = statusCode;
            this.cuerpo = cuerpo;
        }
    }

    /** Envía el XML SOAP y devuelve el código HTTP junto con el body crudo de la respuesta. */
    public static Respuesta enviar(String soapXml) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(URL))
                .timeout(TIMEOUT)
                .header("Content-Type", CONTENT_TYPE)
                .header("SOAPAction", SOAP_ACTION)
                .POST(HttpRequest.BodyPublishers.ofString(soapXml, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        return new Respuesta(response.statusCode(), response.body());
    }
}
