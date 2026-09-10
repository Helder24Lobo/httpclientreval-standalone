package com.example.httpclientreval.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

/**
 * Estructura fija del JSON de negocio que va cifrado dentro de "_mensaje"
 * (recarga tipo Comcel). Los nombres de campo son fijos; los valores se
 * llenan campo por campo en cada corrida (ver Main.encrypt). Los valores
 * puestos aquí por defecto son los últimos conocidos/típicos, no obligatorios.
 */
public class MensajeNegocio {

    private static final Gson GSON_PRETTY = new GsonBuilder().setPrettyPrinting().create();

    public static class BodyMensaje {
        @SerializedName("Autorizacion")
        public String autorizacion = "";

        @SerializedName("CodBarras")
        public String codBarras = "";

        @SerializedName("Convenio")
        public String convenio = "";

        @SerializedName("FechaVencimiento")
        public String fechaVencimiento = "";

        @SerializedName("Iac")
        public String iac = "";

        @SerializedName("IdPersona")
        public String idPersona = "226";

        @SerializedName("IdTransaccion")
        public String idTransaccion = "";

        @SerializedName("NoIdentificacionUsuario")
        public String noIdentificacionUsuario = "";

        @SerializedName("NombreUsuario")
        public String nombreUsuario = "";

        @SerializedName("NumCelular")
        public String numCelular = "";

        @SerializedName("Observacion")
        public String observacion = "";

        @SerializedName("Otp")
        public String otp = "";

        @SerializedName("Referencia1")
        public String referencia1 = "";

        @SerializedName("Referencia2")
        public String referencia2 = "";

        @SerializedName("Referencia3")
        public String referencia3 = "";

        @SerializedName("Referencia4")
        public String referencia4 = "";

        @SerializedName("Referencia5")
        public String referencia5 = "";

        @SerializedName("Referencia6")
        public String referencia6 = "";

        @SerializedName("Referencia7")
        public String referencia7 = "";

        @SerializedName("Referencia8")
        public String referencia8 = "";

        @SerializedName("Referencia9")
        public String referencia9 = "";

        @SerializedName("Referencia10")
        public String referencia10 = "";

        @SerializedName("Referencia11")
        public String referencia11 = "";

        @SerializedName("Referencia12")
        public String referencia12 = "0";

        @SerializedName("Referencia13")
        public String referencia13 = "0";

        @SerializedName("Referencia14")
        public String referencia14 = "0";

        @SerializedName("Referencia15")
        public String referencia15 = "3";

        @SerializedName("TipoIdentificacion")
        public String tipoIdentificacion = "";

        @SerializedName("Valor")
        public String valor = "";
    }

    public static class HeaderMensaje {
        @SerializedName("NoIdentificacionCajero")
        public String noIdentificacionCajero = "H3R000200014598";
    }

    private static class Body {
        @SerializedName("_BodyMensaje")
        BodyMensaje bodyMensaje;

        @SerializedName("_headerMensaje")
        HeaderMensaje headerMensaje;
    }

    private static class Mensaje {
        @SerializedName("_body")
        Body body;
    }

    private MensajeNegocio() {
    }

    /** Arma el JSON de negocio en claro a partir del body y header ya completados. */
    public static String build(BodyMensaje bodyMensaje, HeaderMensaje headerMensaje) {
        Body body = new Body();
        body.bodyMensaje = bodyMensaje;
        body.headerMensaje = headerMensaje;

        Mensaje mensaje = new Mensaje();
        mensaje.body = body;

        return GSON_PRETTY.toJson(mensaje);
    }
}
