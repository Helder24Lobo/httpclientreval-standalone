package com.example.httpclientreval.model;

/**
 * Arma el sobre SOAP completo (WS-Security + tem:OBJRequest/tem:data), listo
 * para pegar directo en el body de Postman. La estructura XML (namespaces,
 * WS-Security, nombre del request) es fija; usuario/clave vienen del perfil
 * y el contenido de &lt;tem:data&gt; es el sobre JSON ya armado (con el
 * _mensaje cifrado).
 */
public class SoapRequestBuilder {

    private SoapRequestBuilder() {
    }

    public static String build(String usuario, String password, String sobreJson) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n"
                + "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                + "xmlns:tem=\"http://tempuri.org/\" "
                + "xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\">\n"
                + "    <soapenv:Header>\n"
                + "        <wsse:Security soapenv:mustUnderstand=\"1\">\n"
                + "            <wsse:UsernameToken>\n"
                + "                <wsse:Username>" + escaparXml(usuario) + "</wsse:Username>\n"
                + "                <wsse:Password>" + escaparXml(password) + "</wsse:Password>\n"
                + "            </wsse:UsernameToken>\n"
                + "        </wsse:Security>\n"
                + "    </soapenv:Header>\n"
                + "    <soapenv:Body>\n"
                + "        <tem:OBJRequest>\n"
                + "            <tem:data>" + escaparXml(sobreJson) + "</tem:data>\n"
                + "        </tem:OBJRequest>\n"
                + "    </soapenv:Body>\n"
                + "</soapenv:Envelope>";
    }

    private static String escaparXml(String texto) {
        if (texto == null) {
            return "";
        }
        return texto
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
