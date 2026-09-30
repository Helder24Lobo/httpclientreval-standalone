package com.example.httpclientreval.model;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.prefs.Preferences;

/**
 * Envía el XML SOAP armado por SoapRequestBuilder directo al webservice RVLRRT, para no depender de
 * copiar/pegar a Postman. La URL, el SOAPAction y el timeout se pueden cambiar en Configuración (por
 * ejemplo para apuntar a producción en vez de a pruebas) sin recompilar; se recuerdan entre corridas.
 * Content-Type sí es fijo: este servicio solo habla XML SOAP.
 */
public class SoapHttpClient {

    public static final String URL_POR_DEFECTO = "https://servicios.reval.co:8110/RVLRRTpruebas/RVLRRT.svc";
    public static final String SOAP_ACTION_POR_DEFECTO = "http://tempuri.org/IRVLRRT/OBJRequest";
    public static final int TIMEOUT_POR_DEFECTO_SEGUNDOS = 30;
    public static final int TIMEOUT_MINIMO_SEGUNDOS = 5;
    public static final int TIMEOUT_MAXIMO_SEGUNDOS = 300;

    private static final String CONTENT_TYPE = "text/xml; charset=utf-8";

    private static final String CLAVE_URL = "entorno.url";
    private static final String CLAVE_SOAP_ACTION = "entorno.soapAction";
    private static final String CLAVE_TIMEOUT_SEGUNDOS = "entorno.timeoutSegundos";
    private static final String CLAVE_MARCADO_PRODUCCION = "entorno.marcadoProduccion";
    private static final Preferences PREFS = Preferences.userNodeForPackage(SoapHttpClient.class);

    /**
     * A qué tipo de ambiente apunta la URL configurada, para poder avisarlo de forma permanente en
     * pantalla y que nadie envíe a producción sin darse cuenta.
     */
    public enum Ambiente {
        /** La URL es exactamente {@link #URL_POR_DEFECTO}: el ambiente de pruebas de siempre. Es la única
         *  forma de llegar a este estado; no se puede "declarar" pruebas con una URL distinta. */
        PRUEBAS,
        /** La URL no es la de pruebas y el usuario marcó explícitamente la casilla de Configuración
         *  confirmando que apunta a producción. */
        PRODUCCION,
        /** La URL no es la de pruebas y nadie confirmó que sea producción: un ambiente propio, de
         *  staging, o simplemente el paso intermedio antes de marcarla como producción. */
        PERSONALIZADO
    }

    /**
     * Cliente único para toda la app: es inmutable y seguro entre hilos, y al reutilizarlo se
     * aprovechan las conexiones ya abiertas (y la sesión TLS) en vez de renegociarlas en cada envío.
     * Como el connectTimeout queda fijado al construirlo, cambiar el timeout reconstruye este campo
     * (perdiendo esa reutilización una sola vez); en el resto de envíos se sigue compartiendo igual.
     */
    private static volatile HttpClient client = construirCliente(getTimeoutSegundos());

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

    public static String getUrl() {
        return PREFS.get(CLAVE_URL, URL_POR_DEFECTO);
    }

    public static String getSoapAction() {
        return PREFS.get(CLAVE_SOAP_ACTION, SOAP_ACTION_POR_DEFECTO);
    }

    public static int getTimeoutSegundos() {
        return PREFS.getInt(CLAVE_TIMEOUT_SEGUNDOS, TIMEOUT_POR_DEFECTO_SEGUNDOS);
    }

    public static Ambiente getAmbiente() {
        if (getUrl().equals(URL_POR_DEFECTO)) {
            return Ambiente.PRUEBAS;
        }
        return PREFS.getBoolean(CLAVE_MARCADO_PRODUCCION, false) ? Ambiente.PRODUCCION : Ambiente.PERSONALIZADO;
    }

    public static boolean isMarcadoComoProduccion() {
        return PREFS.getBoolean(CLAVE_MARCADO_PRODUCCION, false);
    }

    /** Solo tiene efecto si la URL actual no es la de pruebas: esa siempre es {@link Ambiente#PRUEBAS}. */
    public static void setMarcadoComoProduccion(boolean marcado) {
        PREFS.putBoolean(CLAVE_MARCADO_PRODUCCION, marcado);
    }

    /** Lanza IllegalArgumentException con mensaje claro si la URL no es http(s) válida; no cambia nada si falla. */
    public static void setUrl(String url) {
        validarUrl(url);
        String urlLimpia = url.trim();
        if (!urlLimpia.equals(getUrl())) {
            // Una URL nueva nunca hereda la confirmación de "es producción" de la URL anterior:
            // hay que volver a marcarla a propósito, para no arrastrar una etiqueta que ya no aplica.
            PREFS.putBoolean(CLAVE_MARCADO_PRODUCCION, false);
        }
        PREFS.put(CLAVE_URL, urlLimpia);
    }

    public static void setSoapAction(String soapAction) {
        if (soapAction == null || soapAction.isBlank()) {
            throw new IllegalArgumentException("El SOAPAction no puede quedar vacío.");
        }
        PREFS.put(CLAVE_SOAP_ACTION, soapAction.trim());
    }

    public static void setTimeoutSegundos(int segundos) {
        if (segundos < TIMEOUT_MINIMO_SEGUNDOS || segundos > TIMEOUT_MAXIMO_SEGUNDOS) {
            throw new IllegalArgumentException("El timeout debe estar entre " + TIMEOUT_MINIMO_SEGUNDOS
                    + " y " + TIMEOUT_MAXIMO_SEGUNDOS + " segundos.");
        }
        PREFS.putInt(CLAVE_TIMEOUT_SEGUNDOS, segundos);
        // El connectTimeout va en el cliente, no en la petición: hay que reconstruirlo para que aplique.
        client = construirCliente(segundos);
    }

    /** Deshace cualquier cambio de entorno y vuelve a apuntar al ambiente de pruebas de siempre. */
    public static void restablecerEntorno() {
        PREFS.remove(CLAVE_URL);
        PREFS.remove(CLAVE_SOAP_ACTION);
        PREFS.remove(CLAVE_TIMEOUT_SEGUNDOS);
        PREFS.remove(CLAVE_MARCADO_PRODUCCION);
        client = construirCliente(TIMEOUT_POR_DEFECTO_SEGUNDOS);
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

    private static HttpClient construirCliente(int timeoutSegundos) {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(timeoutSegundos))
                .build();
    }

    /** Envía el XML SOAP y devuelve el código HTTP, el body crudo y cuánto tardó la petición. */
    public static Respuesta enviar(String soapXml) throws IOException, InterruptedException {
        Duration timeout = Duration.ofSeconds(getTimeoutSegundos());
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getUrl()))
                .timeout(timeout)
                .header("Content-Type", CONTENT_TYPE)
                .header("SOAPAction", getSoapAction())
                .POST(HttpRequest.BodyPublishers.ofString(soapXml, StandardCharsets.UTF_8))
                .build();

        long inicio = System.currentTimeMillis();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        long tiempoMs = System.currentTimeMillis() - inicio;

        return new Respuesta(response.statusCode(), response.body(), tiempoMs);
    }

    /**
     * El mismo POST que hace {@link #enviar}, como comando cURL (sintaxis bash: comillas simples,
     * pensado para pegar en una terminal o en Postman/Insomnia vía "importar cURL"). Se genera aquí,
     * junto al código que arma la petición real, para que ambos no se desincronicen si cambian la
     * URL, el SOAPAction o el Content-Type.
     */
    public static String curlPara(String soapXml) {
        return "curl -X POST '" + getUrl() + "' \\\n"
                + "  -H 'Content-Type: " + CONTENT_TYPE + "' \\\n"
                + "  -H 'SOAPAction: " + getSoapAction() + "' \\\n"
                + "  --data-raw '" + escaparComillaSimple(soapXml == null ? "" : soapXml) + "'";
    }

    /** Escapado seguro para comillas simples en bash: cierra la comilla, escapa una comilla literal y la reabre. */
    private static String escaparComillaSimple(String texto) {
        return texto.replace("'", "'\\''");
    }
}
