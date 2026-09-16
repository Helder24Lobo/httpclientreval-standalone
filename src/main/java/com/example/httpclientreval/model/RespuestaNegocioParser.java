package com.example.httpclientreval.model;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Lee el código de negocio (header.Codigo) de la respuesta ya descifrada del
 * WS — el HTTP casi siempre es 200 aunque el negocio rechace la transacción
 * (ej. "SU BANCO NO RESPONDE"), así que el resultado real está acá.
 */
public class RespuestaNegocioParser {

    private RespuestaNegocioParser() {
    }

    /** Devuelve header.Codigo, o null si el JSON no tiene esa forma. */
    public static Integer extraerCodigo(String jsonRespuesta) {
        try {
            JsonObject raiz = JsonParser.parseString(jsonRespuesta).getAsJsonObject();
            JsonObject header = raiz.getAsJsonObject("header");
            return header.get("Codigo").getAsInt();
        } catch (Exception e) {
            return null;
        }
    }
}
