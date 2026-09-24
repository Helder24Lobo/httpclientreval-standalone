package com.example.httpclientreval.ui;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringReader;
import java.io.StringWriter;

/**
 * Detecta si un texto es JSON o XML y lo devuelve con formato (pretty-print).
 * Si el texto no es ninguno de los dos, o no es válido, se deja tal cual.
 */
final class FormatoSalida {

    enum Tipo { JSON, XML, TEXTO }

    /** Texto ya formateado junto con el tipo detectado. */
    static final class Resultado {
        final String texto;
        final Tipo tipo;

        Resultado(String texto, Tipo tipo) {
            this.texto = texto;
            this.tipo = tipo;
        }
    }

    private FormatoSalida() {
    }

    static Resultado formatear(String texto) {
        if (texto == null || texto.isBlank()) {
            return new Resultado(texto == null ? "" : texto, Tipo.TEXTO);
        }
        String recortado = texto.strip();
        char inicio = recortado.charAt(0);
        try {
            if (inicio == '{' || inicio == '[') {
                return new Resultado(formatearJson(recortado), Tipo.JSON);
            }
            if (inicio == '<') {
                return new Resultado(formatearXml(recortado), Tipo.XML);
            }
        } catch (Exception noEsValido) {
            // Se muestra el texto original: mejor eso que ocultar una respuesta mal formada.
        }
        return new Resultado(texto, Tipo.TEXTO);
    }

    static String formatearJson(String json) {
        JsonElement elemento = JsonParser.parseString(json);
        // Sin esto Gson escapa <, >, &, = y ' (<...) y omite los null.
        return new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().serializeNulls().create().toJson(elemento);
    }

    static String formatearXml(String xml) throws Exception {
        DocumentBuilderFactory fabrica = DocumentBuilderFactory.newInstance();
        fabrica.setNamespaceAware(true);
        // La respuesta viene de un servidor externo: sin DTD/entidades externas (XXE).
        fabrica.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        fabrica.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        Document documento = fabrica.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        quitarEspaciosSobrantes(documento);

        Transformer transformador = TransformerFactory.newInstance().newTransformer();
        transformador.setOutputProperty(OutputKeys.INDENT, "yes");
        transformador.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        // La declaración se agrega a mano: el transformador la deja pegada a la raíz, en la misma línea.
        transformador.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        StringWriter salida = new StringWriter();
        transformador.transform(new DOMSource(documento), new StreamResult(salida));
        // El transformador usa el separador del SO; Swing trabaja con \n.
        String cuerpo = salida.toString().replace("\r\n", "\n").strip();
        int finDeclaracion = xml.startsWith("<?xml") ? xml.indexOf("?>") : -1;
        return finDeclaracion < 0 ? cuerpo : xml.substring(0, finDeclaracion + 2) + "\n" + cuerpo;
    }

    /** Quita los nodos de solo espacios para que el indentado no duplique los saltos de línea ya existentes. */
    private static void quitarEspaciosSobrantes(Node nodo) {
        NodeList hijos = nodo.getChildNodes();
        for (int i = hijos.getLength() - 1; i >= 0; i--) {
            Node hijo = hijos.item(i);
            if (hijo.getNodeType() == Node.TEXT_NODE && hijo.getTextContent().isBlank()) {
                nodo.removeChild(hijo);
            } else {
                quitarEspaciosSobrantes(hijo);
            }
        }
    }
}
