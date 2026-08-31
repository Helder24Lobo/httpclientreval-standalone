package com.example.httpclientreval.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

/**
 * Modelo del sobre que va dentro de <tem:data>:
 * {
 *   "_header": { "IdTransaccion": 1, "Idcliente": 26, "IpCliente": "172.17.0.4" },
 *   "_body":   { "_mensaje": "<JSON de negocio cifrado en base64>" }
 * }
 *
 * Equivalente a MRequestBodyRrt del proyecto Android, usando Gson.
 */
public class Envelope {

    private static final Gson GSON_COMPACT = new Gson();
    private static final Gson GSON_PRETTY = new GsonBuilder().setPrettyPrinting().create();

    public static class Header {
        @SerializedName("IdTransaccion")
        public int idTransaccion;

        @SerializedName("Idcliente")
        public int idCliente;

        @SerializedName("IpCliente")
        public String ipCliente;

        public Header(int idTransaccion, int idCliente, String ipCliente) {
            this.idTransaccion = idTransaccion;
            this.idCliente = idCliente;
            this.ipCliente = ipCliente;
        }
    }

    public static class Body {
        @SerializedName("_mensaje")
        public String mensaje;

        public Body(String mensaje) {
            this.mensaje = mensaje;
        }
    }

    public static class Sobre {
        @SerializedName("_header")
        public Header header;

        @SerializedName("_body")
        public Body body;

        public Sobre(Header header, Body body) {
            this.header = header;
            this.body = body;
        }
    }

    private Envelope() {
    }

    /** Arma el sobre completo en JSON compacto (una sola línea), listo para <tem:data>. */
    public static String build(String mensajeCifradoBase64, int idCliente, int idTransaccion, String ipCliente) {
        Sobre sobre = new Sobre(new Header(idTransaccion, idCliente, ipCliente), new Body(mensajeCifradoBase64));
        return GSON_COMPACT.toJson(sobre);
    }

    /** Igual que build(), pero con indentación (útil solo para inspección visual). */
    public static String buildPretty(String mensajeCifradoBase64, int idCliente, int idTransaccion, String ipCliente) {
        Sobre sobre = new Sobre(new Header(idTransaccion, idCliente, ipCliente), new Body(mensajeCifradoBase64));
        return GSON_PRETTY.toJson(sobre);
    }

    /** Parsea el sobre JSON completo a un objeto Sobre. */
    public static Sobre parse(String sobreJson) {
        Sobre sobre = GSON_COMPACT.fromJson(sobreJson, Sobre.class);
        if (sobre == null) {
            throw new IllegalArgumentException("No se pudo parsear el sobre JSON: " + sobreJson);
        }
        return sobre;
    }

    /** Extrae directamente _body._mensaje de un sobre JSON completo. */
    public static String extraerMensaje(String sobreJson) {
        Sobre sobre = parse(sobreJson);
        if (sobre.body == null || sobre.body.mensaje == null) {
            throw new IllegalArgumentException("El JSON no contiene _body._mensaje.");
        }
        return sobre.body.mensaje;
    }
}
