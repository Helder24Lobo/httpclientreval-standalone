package com.example.httpclientreval.model;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prueba {@link SoapHttpClient#enviar} contra un servidor HTTP real (el embebido del JDK,
 * {@link HttpServer}), no solo la construcción del comando cURL o la validación de la URL.
 *
 * enviar() lee URL/SOAPAction/timeout de las preferencias reales del usuario (mismas que usa la app),
 * así que cada prueba guarda esos valores en @BeforeEach y los restaura en @AfterEach: al terminar
 * ./gradlew test, el entorno configurado en la app queda exactamente como estaba antes de correrlas.
 */
class SoapHttpClientEnviarTest {

    private HttpServer servidor;
    private String urlOriginal;
    private String soapActionOriginal;
    private int timeoutOriginal;
    private boolean marcadoProduccionOriginal;

    @BeforeEach
    void guardarEntornoActual() {
        urlOriginal = SoapHttpClient.getUrl();
        soapActionOriginal = SoapHttpClient.getSoapAction();
        timeoutOriginal = SoapHttpClient.getTimeoutSegundos();
        marcadoProduccionOriginal = SoapHttpClient.isMarcadoComoProduccion();
    }

    @AfterEach
    void restaurarEntorno() {
        if (servidor != null) {
            servidor.stop(0);
        }
        SoapHttpClient.setUrl(urlOriginal);
        SoapHttpClient.setSoapAction(soapActionOriginal);
        SoapHttpClient.setTimeoutSegundos(timeoutOriginal);
        SoapHttpClient.setMarcadoComoProduccion(marcadoProduccionOriginal);
    }

    /** Arranca un servidor local en el puerto 0 (el SO elige uno libre) y apunta SoapHttpClient ahí. */
    private void apuntarA(String contexto, com.sun.net.httpserver.HttpHandler manejador) throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext(contexto, manejador);
        servidor.start();
        SoapHttpClient.setUrl("http://127.0.0.1:" + servidor.getAddress().getPort() + contexto);
    }

    private static void responder(com.sun.net.httpserver.HttpExchange ex, int status, String cuerpo) throws IOException {
        byte[] bytes = cuerpo.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(status, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.close();
    }

    @Test
    void respuestaExitosaDevuelveElCuerpoYElCodigo200() throws Exception {
        String cuerpoEsperado = "<s:Envelope xmlns:s=\"x\"><s:Body>ok</s:Body></s:Envelope>";
        AtomicReference<String> soapActionRecibido = new AtomicReference<>();
        AtomicReference<String> contentTypeRecibido = new AtomicReference<>();
        apuntarA("/ws", ex -> {
            soapActionRecibido.set(ex.getRequestHeaders().getFirst("SOAPAction"));
            contentTypeRecibido.set(ex.getRequestHeaders().getFirst("Content-Type"));
            responder(ex, 200, cuerpoEsperado);
        });

        SoapHttpClient.Respuesta respuesta = SoapHttpClient.enviar("<peticion/>");

        assertEquals(200, respuesta.statusCode);
        assertEquals(cuerpoEsperado, respuesta.cuerpo);
        assertTrue(respuesta.tiempoMs >= 0);
        assertEquals(SoapHttpClient.getSoapAction(), soapActionRecibido.get());
        assertEquals("text/xml; charset=utf-8", contentTypeRecibido.get());
    }

    @Test
    void error500ConSoapFaultSeDevuelveTalCualSinLanzar() throws Exception {
        String fault = "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                + "<s:Body><s:Fault><faultstring>Error interno del servicio</faultstring></s:Fault></s:Body>"
                + "</s:Envelope>";
        apuntarA("/ws", ex -> responder(ex, 500, fault));

        SoapHttpClient.Respuesta respuesta = SoapHttpClient.enviar("<peticion/>");

        // Un 500 no es una excepción para el cliente HTTP: el status y el cuerpo quedan tal cual,
        // para que quien llame (EnvioRegistrado) decida qué significa.
        assertEquals(500, respuesta.statusCode);
        assertEquals(fault, respuesta.cuerpo);
    }

    @Test
    void xmlMalformadoEnLaRespuestaSeDevuelveSinIntentarParsearlo() throws Exception {
        String cuerpoRoto = "<s:Envelope><s:Body>falta cerrar las etiquetas";
        apuntarA("/ws", ex -> responder(ex, 200, cuerpoRoto));

        SoapHttpClient.Respuesta respuesta = SoapHttpClient.enviar("<peticion/>");

        // enviar() transporta bytes, no valida XML; eso lo hace SoapResponseParser más arriba.
        assertEquals(200, respuesta.statusCode);
        assertEquals(cuerpoRoto, respuesta.cuerpo);
    }

    @Test
    void timeoutLanzaHttpTimeoutExceptionSiElServidorNoResponde() throws Exception {
        CountDownLatch nuncaSeCuentaAbajo = new CountDownLatch(1);
        apuntarA("/ws", ex -> {
            try {
                // Retiene la conexión abierta más allá del timeout del cliente, sin responder nunca.
                nuncaSeCuentaAbajo.await(20, TimeUnit.SECONDS);
            } catch (InterruptedException ignorada) {
                Thread.currentThread().interrupt();
            } finally {
                ex.close();
            }
        });
        SoapHttpClient.setTimeoutSegundos(SoapHttpClient.TIMEOUT_MINIMO_SEGUNDOS);

        long inicio = System.currentTimeMillis();
        assertThrows(HttpTimeoutException.class, () -> SoapHttpClient.enviar("<peticion/>"));
        long duracionMs = System.currentTimeMillis() - inicio;

        // Debe fallar cerca del timeout configurado (5s), no quedarse colgado ni fallar al instante.
        assertTrue(duracionMs >= SoapHttpClient.TIMEOUT_MINIMO_SEGUNDOS * 1000L - 500,
                "tardó " + duracionMs + "ms, antes de lo esperado");
        assertTrue(duracionMs < (SoapHttpClient.TIMEOUT_MINIMO_SEGUNDOS + 10) * 1000L,
                "tardó " + duracionMs + "ms, más de lo esperado");
    }
}
