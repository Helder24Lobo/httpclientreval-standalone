package com.example.httpclientreval.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Solo cubre {@link SoapHttpClient#validarUrl}, que es puro (no toca Preferences). Los métodos
 * setUrl/setSoapAction/setTimeoutSegundos sí persisten en las preferencias reales del usuario
 * (mismo patrón que TemaPreferencias/FuentePreferencias), así que no se ejercitan en pruebas.
 */
class SoapHttpClientValidarUrlTest {

    @Test
    void aceptaHttpYHttps() {
        assertDoesNotThrow(() -> SoapHttpClient.validarUrl("https://servicios.reval.co:8110/RVLRRT.svc"));
        assertDoesNotThrow(() -> SoapHttpClient.validarUrl("http://192.168.1.10:8080/ws"));
    }

    @Test
    void rechazaVaciaONula() {
        assertThrows(IllegalArgumentException.class, () -> SoapHttpClient.validarUrl(""));
        assertThrows(IllegalArgumentException.class, () -> SoapHttpClient.validarUrl("   "));
        assertThrows(IllegalArgumentException.class, () -> SoapHttpClient.validarUrl(null));
    }

    @Test
    void rechazaEsquemaDistintoDeHttp() {
        assertThrows(IllegalArgumentException.class, () -> SoapHttpClient.validarUrl("ftp://servidor/archivo"));
    }

    @Test
    void rechazaTextoQueNoEsUnaUrl() {
        assertThrows(IllegalArgumentException.class, () -> SoapHttpClient.validarUrl("esto no es una url"));
    }

    @Test
    void rechazaSintaxisInvalida() {
        // Un espacio sin codificar dentro de la URL es sintaxis inválida para java.net.URI.
        assertThrows(IllegalArgumentException.class, () -> SoapHttpClient.validarUrl("https://servidor/ruta con espacios"));
    }
}
