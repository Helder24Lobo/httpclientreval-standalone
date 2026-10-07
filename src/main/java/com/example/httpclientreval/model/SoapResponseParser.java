package com.example.httpclientreval.model;

import com.example.httpclientreval.util.Registro;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

/**
 * Extrae el contenido de <OBJRequestResult> de la respuesta SOAP del WS
 * (sin importar el prefijo de namespace que use el response), para poder
 * descifrarlo con la misma llave AES del perfil.
 */
public class SoapResponseParser {

    private SoapResponseParser() {
    }

    /** Devuelve el texto de OBJRequestResult, o null si el XML no trae ese elemento o no se pudo parsear. */
    public static String extraerObjRequestResult(String xmlRespuesta) {
        if (xmlRespuesta == null || xmlRespuesta.isBlank()) {
            return null;
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(xmlRespuesta.getBytes(StandardCharsets.UTF_8)));

            NodeList todos = doc.getElementsByTagName("*");
            for (int i = 0; i < todos.getLength(); i++) {
                Node nodo = todos.item(i);
                if ("OBJRequestResult".equals(nodo.getLocalName())) {
                    return nodo.getTextContent();
                }
            }
            return null;
        } catch (Exception e) {
            // Rutinario: la respuesta puede no ser XML válido (ej. un SOAP Fault con otra forma,
            // o el WS devolvió HTML de error); se trata igual que "no trae OBJRequestResult".
            Registro.advertencia("La respuesta HTTP no se pudo parsear como XML al buscar OBJRequestResult", e);
            return null;
        }
    }
}
