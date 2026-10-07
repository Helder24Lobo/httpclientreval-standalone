package com.example.httpclientreval.model;

import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Envía el XML SOAP armado por SoapRequestBuilder directo al webservice RVLRRT, para no depender de
 * copiar/pegar a Postman. La URL, el SOAPAction y el timeout salen del ambiente activo (ver
 * {@link Entornos}), que se administra desde la app sin recompilar. Content-Type sí es fijo: este
 * servicio solo habla XML SOAP.
 */
public class SoapHttpClient {

    /** Valores del ambiente de Pruebas (el que se crea la primera vez). */
    public static final String URL_POR_DEFECTO = "https://servicios.reval.co:8110/RVLRRTpruebas/RVLRRT.svc";
    public static final String SOAP_ACTION_POR_DEFECTO = "http://tempuri.org/IRVLRRT/OBJRequest";
    public static final int TIMEOUT_POR_DEFECTO_SEGUNDOS = 30;
    public static final int TIMEOUT_MINIMO_SEGUNDOS = 5;
    public static final int TIMEOUT_MAXIMO_SEGUNDOS = 300;

    private static final String CONTENT_TYPE = "text/xml; charset=utf-8";

    /**
     * A qué tipo de ambiente apunta el activo, para avisarlo de forma permanente en pantalla y que nadie
     * envíe a producción sin darse cuenta.
     */
    public enum Ambiente {
        /** La URL es exactamente {@link #URL_POR_DEFECTO} y no está marcado como producción. */
        PRUEBAS,
        /** El ambiente activo está marcado explícitamente como producción. */
        PRODUCCION,
        /** Cualquier otro (desarrollo, staging, un servidor propio...). */
        PERSONALIZADO
    }

    /** Solo para pruebas: ambiente que se usa en vez del activo de {@link Entornos}. */
    private static volatile Entorno entornoForzado;

    /**
     * Cliente compartido: es inmutable y seguro entre hilos, y al reutilizarlo se aprovechan las
     * conexiones ya abiertas (y la sesión TLS). Como el connectTimeout queda fijado al construirlo, se
     * reconstruye solo cuando cambia el timeout del ambiente (p. ej. al activar otro).
     */
    private static HttpClient client;
    private static int timeoutDelCliente = -1;

    private SoapHttpClient() {
    }

    public static class Respuesta {
        public final int statusCode;
        public final String cuerpo;
        public final long tiempoMs;

        public Respuesta(int statusCode, String cuerpo, long tiempoMs) {
            this.statusCode = statusCode;
            this.cuerpo = cuerpo;
            this.tiempoMs = tiempoMs;
        }
    }

    /** El ambiente al que se está apuntando ahora. */
    public static Entorno entornoActivo() {
        Entorno forzado = entornoForzado;
        return forzado != null ? forzado : Entornos.instancia().activo();
    }

    /** Solo para pruebas: {@code null} vuelve al ambiente activo real. */
    static void forzarEntorno(Entorno entorno) {
        entornoForzado = entorno;
    }

    public static String getUrl() {
        return entornoActivo().url;
    }

    public static String getSoapAction() {
        return entornoActivo().soapAction;
    }

    public static int getTimeoutSegundos() {
        return entornoActivo().timeoutSegundos;
    }

    public static Ambiente getAmbiente() {
        return ambienteDe(entornoActivo());
    }

    public static Ambiente ambienteDe(Entorno entorno) {
        if (entorno.produccion) {
            return Ambiente.PRODUCCION;
        }
        return URL_POR_DEFECTO.equals(entorno.url) ? Ambiente.PRUEBAS : Ambiente.PERSONALIZADO;
    }

    /** Lanza IllegalArgumentException con mensaje claro si {@code url} no es una URL http(s) válida. */
    public static void validarUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("La URL del servicio no puede quedar vacía.");
        }
        URI uri;
        try {
            uri = new URI(url.trim());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("La URL no es válida: " + e.getMessage());
        }
        String esquema = uri.getScheme();
        if (esquema == null || !(esquema.equalsIgnoreCase("http") || esquema.equalsIgnoreCase("https"))) {
            throw new IllegalArgumentException("La URL debe empezar con http:// o https://");
        }
        if (uri.getHost() == null) {
            throw new IllegalArgumentException("La URL no tiene un host válido.");
        }
    }

    private static synchronized HttpClient clienteParaTimeout(int timeoutSegundos) {
        if (client == null || timeoutDelCliente != timeoutSegundos) {
            client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(timeoutSegundos))
                    .build();
            timeoutDelCliente = timeoutSegundos;
        }
        return client;
    }

    /** Envía el XML SOAP al ambiente activo y devuelve el código HTTP, el body crudo y cuánto tardó la petición. */
    public static Respuesta enviar(String soapXml) throws IOException, InterruptedException {
        // Una sola lectura del ambiente: URL, SOAPAction y timeout salen del mismo, aunque lo cambien en medio.
        Entorno entorno = entornoActivo();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(entorno.url))
                .timeout(Duration.ofSeconds(entorno.timeoutSegundos))
                .header("Content-Type", CONTENT_TYPE)
                .header("SOAPAction", entorno.soapAction)
                .POST(HttpRequest.BodyPublishers.ofString(soapXml, StandardCharsets.UTF_8))
                .build();

        long inicio = System.currentTimeMillis();
        HttpResponse<String> response = enviarConReintento(clienteParaTimeout(entorno.timeoutSegundos), request);
        long tiempoMs = System.currentTimeMillis() - inicio;

        return new Respuesta(response.statusCode(), response.body(), tiempoMs);
    }

    private static final int INTENTOS_HANDSHAKE = 3;
    private static final long ESPERA_ENTRE_INTENTOS_MS = 400;

    /**
     * El servidor corta de vez en cuando el handshake TLS ("Remote host terminated the handshake"). Eso pasa
     * antes de que se envíe la petición, así que repetir es seguro (no puede duplicar una transacción); solo
     * se reintenta ese error, cualquier otro falla de inmediato.
     */
    private static HttpResponse<String> enviarConReintento(HttpClient cliente, HttpRequest request)
            throws IOException, InterruptedException {
        for (int intento = 1; ; intento++) {
            try {
                return cliente.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (SSLHandshakeException e) {
                if (intento >= INTENTOS_HANDSHAKE) {
                    throw e;
                }
                Thread.sleep(ESPERA_ENTRE_INTENTOS_MS);
            }
        }
    }

    /**
     * El mismo POST que hace {@link #enviar}, como comando cURL (sintaxis bash: comillas simples,
     * pensado para pegar en una terminal o en Postman/Insomnia vía "importar cURL"). Se genera aquí,
     * junto al código que arma la petición real, para que ambos no se desincronicen si cambian la
     * URL, el SOAPAction o el Content-Type.
     */
    public static String curlPara(String soapXml) {
        Entorno entorno = entornoActivo();
        return "curl -X POST '" + entorno.url + "' \\\n"
                + "  -H 'Content-Type: " + CONTENT_TYPE + "' \\\n"
                + "  -H 'SOAPAction: " + entorno.soapAction + "' \\\n"
                + "  --data-raw '" + escaparComillaSimple(soapXml == null ? "" : soapXml) + "'";
    }

    /** Escapado seguro para comillas simples en bash: cierra la comilla, escapa una comilla literal y la reabre. */
    private static String escaparComillaSimple(String texto) {
        return texto.replace("'", "'\\''");
    }
}
