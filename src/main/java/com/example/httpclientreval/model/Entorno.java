package com.example.httpclientreval.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Un ambiente al que la app puede apuntar (Pruebas, Producción, Desarrollo...): dónde está el servicio
 * (URL, SOAPAction, timeout) y con qué credenciales se habla con él (llave AES y usuario/contraseña
 * WS-Security). Cada ambiente tiene las suyas, porque lo que sirve en pruebas no sirve en producción.
 *
 * Un ambiente puede guardarse incompleto (p. ej. Producción mientras no hay credenciales) pero no se
 * puede activar hasta que {@link #faltantes()} esté vacío.
 */
public class Entorno {

    public String nombre = "";
    public String url = "";
    public String soapAction = SoapHttpClient.SOAP_ACTION_POR_DEFECTO;
    public int timeoutSegundos = SoapHttpClient.TIMEOUT_POR_DEFECTO_SEGUNDOS;
    public String llaveAes = "";
    public String wsseUsername = "";
    public String wssePassword = "";
    /** Marca explícita de producción: dispara el rótulo rojo y la confirmación antes de enviar. */
    public boolean produccion = false;

    public Entorno() {
    }

    public Entorno(String nombre) {
        this.nombre = nombre;
    }

    /** Qué le falta para poder usarse (vacío = listo). Los textos sirven tal cual para mostrar al usuario. */
    public List<String> faltantes() {
        List<String> faltan = new ArrayList<>();
        if (esVacio(url)) {
            faltan.add("la URL del servicio");
        }
        if (esVacio(soapAction)) {
            faltan.add("el SOAPAction");
        }
        if (esVacio(llaveAes)) {
            faltan.add("la llave AES");
        }
        if (esVacio(wsseUsername)) {
            faltan.add("el usuario WSSE");
        }
        if (esVacio(wssePassword)) {
            faltan.add("la contraseña WSSE");
        }
        return faltan;
    }

    public boolean listoParaUsar() {
        return faltantes().isEmpty();
    }

    public Entorno copia() {
        Entorno c = new Entorno(nombre);
        c.url = url;
        c.soapAction = soapAction;
        c.timeoutSegundos = timeoutSegundos;
        c.llaveAes = llaveAes;
        c.wsseUsername = wsseUsername;
        c.wssePassword = wssePassword;
        c.produccion = produccion;
        return c;
    }

    /**
     * Huella de todo lo que cambia lo que se envía (URL, SOAPAction, llave, usuario, contraseña): sirve para
     * detectar que el ambiente cambió desde que se generó una petición. Es solo para comparar en memoria;
     * nunca se escribe en disco ni en el log (contiene secretos).
     */
    public String firma() {
        // Separador que no aparece en ningún campo, para que "a|bc" y "ab|c" no den la misma huella.
        return String.join("\u0001", nombre, url, soapAction, llaveAes, wsseUsername, wssePassword);
    }

    @Override
    public String toString() {
        return nombre;
    }

    static boolean esVacio(String texto) {
        return texto == null || texto.isBlank();
    }
}
