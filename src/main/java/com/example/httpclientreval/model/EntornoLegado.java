package com.example.httpclientreval.model;

import java.util.prefs.Preferences;

/**
 * Lee (sin modificar nada) el entorno que las versiones anteriores guardaban en las preferencias del
 * usuario: una sola URL/SOAPAction/timeout editable en Configuración. Sirve solo para no perder esa
 * configuración al pasar al módulo de ambientes: si había una URL distinta de la de pruebas, se
 * convierte en un ambiente "Personalizado".
 */
final class EntornoLegado {

    private EntornoLegado() {
    }

    /** @return el ambiente que describían las preferencias, o {@code null} si usaban la URL de pruebas de siempre. */
    static Entorno leer() {
        try {
            Preferences prefs = Preferences.userNodeForPackage(SoapHttpClient.class);
            String url = prefs.get("entorno.url", SoapHttpClient.URL_POR_DEFECTO);
            if (url.equals(SoapHttpClient.URL_POR_DEFECTO)) {
                return null;
            }
            Entorno e = new Entorno("Personalizado");
            e.url = url;
            e.soapAction = prefs.get("entorno.soapAction", SoapHttpClient.SOAP_ACTION_POR_DEFECTO);
            e.timeoutSegundos = prefs.getInt("entorno.timeoutSegundos", SoapHttpClient.TIMEOUT_POR_DEFECTO_SEGUNDOS);
            e.produccion = prefs.getBoolean("entorno.marcadoProduccion", false);
            return e;
        } catch (RuntimeException sinPreferencias) {
            return null;
        }
    }
}
