package com.example.httpclientreval.model;

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
            return null;
        }
    }
}
