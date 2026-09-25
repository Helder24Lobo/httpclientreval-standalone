package com.example.httpclientreval.model;

import com.example.httpclientreval.crypto.AES256CBC;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnvioRegistradoTest {

    private static final String LLAVE = "12345678901234567890123456789012";

    private static Profile perfil() {
        Profile p = new Profile();
        p.nombre = "Recaudos - Prueba";
        p.llaveAes = LLAVE;
        return p;
    }

    private static String soapConResultado(String resultadoCifrado) {
        return "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\"><s:Body>"
                + "<OBJRequestResponse xmlns=\"http://tempuri.org/\">"
                + "<OBJRequestResult>" + resultadoCifrado + "</OBJRequestResult>"
                + "</OBJRequestResponse></s:Body></s:Envelope>";
    }

    private static EnvioRegistrado analizar(int status, String cuerpo) {
        return EnvioRegistrado.desde(LocalDateTime.now(), perfil(), "<soap/>",
                new SoapHttpClient.Respuesta(status, cuerpo, 120));
    }

    @Test
    void codigoCeroEsExito() throws Exception {
        String cifrado = AES256CBC.encryptWithRandomIV("{\"header\":{\"Codigo\":0,\"Mensaje\":\"OK\"}}", LLAVE);
        EnvioRegistrado e = analizar(200, soapConResultado(cifrado));

        assertEquals(EnvioRegistrado.Resultado.EXITO, e.resultado());
        assertEquals("Código 0", e.resumen());
        assertEquals("OK", e.mensajeNegocio);
        assertEquals("<soap/>", e.soapEnviado);
        assertEquals("Recaudos - Prueba", e.perfilNombre);
    }

    @Test
    void codigoDistintoDeCeroEsErrorDeNegocio() throws Exception {
        String cifrado = AES256CBC.encryptWithRandomIV("{\"header\":{\"Codigo\":99,\"Mensaje\":\"SU BANCO NO RESPONDE\"}}", LLAVE);
        EnvioRegistrado e = analizar(200, soapConResultado(cifrado));

        assertEquals(EnvioRegistrado.Resultado.ERROR_NEGOCIO, e.resultado());
        assertEquals("Código 99", e.resumen());
    }

    @Test
    void httpFueraDeDosCientosEsErrorHttpAunqueHayaCuerpo() {
        EnvioRegistrado e = analizar(500, "<fault/>");

        assertEquals(EnvioRegistrado.Resultado.ERROR_HTTP, e.resultado());
        assertEquals("HTTP 500", e.resumen());
    }

    @Test
    void sinObjRequestResultEsIncompleto() {
        EnvioRegistrado e = analizar(200, "<s:Envelope xmlns:s=\"x\"><s:Body/></s:Envelope>");

        assertEquals(EnvioRegistrado.Resultado.INCOMPLETO, e.resultado());
        assertNull(e.resultadoCifrado);
        assertEquals("Sin OBJRequestResult", e.resumen());
    }

    @Test
    void resultadoQueNoSeDescifraConLaLlaveDelPerfilEsIncompletoConMotivo() {
        EnvioRegistrado e = analizar(200, soapConResultado("AAAA"));

        assertEquals(EnvioRegistrado.Resultado.INCOMPLETO, e.resultado());
        assertEquals("AAAA", e.resultadoCifrado);
        assertNull(e.resultadoPlano);
        assertTrue(e.errorDescifrado != null);
    }

    @Test
    void fallaDeConexionQuedaSinRespuestaConElMotivo() {
        EnvioRegistrado e = EnvioRegistrado.fallido(LocalDateTime.now(), perfil(), "<soap/>",
                new java.net.ConnectException("Connection refused"), 45);

        assertEquals(EnvioRegistrado.Resultado.SIN_RESPUESTA, e.resultado());
        assertNull(e.statusCode);
        assertEquals("Connection refused", e.errorEnvio);
        assertEquals("Sin respuesta", e.resumen());
    }
}
