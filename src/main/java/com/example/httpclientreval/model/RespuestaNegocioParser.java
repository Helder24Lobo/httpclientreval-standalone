package com.example.httpclientreval.model;

import com.example.httpclientreval.util.Registro;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Lee el código y mensaje de negocio (header.Codigo / _header.Codigo) de la respuesta
 * ya descifrada del WS — el HTTP casi siempre es 200 aunque el negocio rechace la
 * transacción (ej. "SU BANCO NO RESPONDE" o Código 99), así que el resultado real está acá.
 */
public class RespuestaNegocioParser {

    private RespuestaNegocioParser() {
    }

    public static class Resultado {
        public final Integer codigo;
        public final String mensaje;

        public Resultado(Integer codigo, String mensaje) {
            this.codigo = codigo;
            this.mensaje = mensaje;
        }

        public boolean esExitoso() {
            return codigo != null && codigo == 0;
        }
    }

    public static Resultado parsear(String jsonRespuesta) {
        try {
            JsonObject raiz = JsonParser.parseString(jsonRespuesta).getAsJsonObject();
            JsonObject header = null;
            if (raiz.has("header") && !raiz.get("header").isJsonNull()) {
                header = raiz.getAsJsonObject("header");
            } else if (raiz.has("_header") && !raiz.get("_header").isJsonNull()) {
                header = raiz.getAsJsonObject("_header");
            }

            if (header != null) {
                Integer codigo = header.has("Codigo") && !header.get("Codigo").isJsonNull()
                        ? header.get("Codigo").getAsInt() : null;
                String mensaje = header.has("Mensaje") && !header.get("Mensaje").isJsonNull()
                        ? header.get("Mensaje").getAsString() : null;
                return new Resultado(codigo, mensaje);
            }
        } catch (Exception e) {
            // Rutinario: pasa siempre que la respuesta en claro no tiene la forma header/_header
            // esperada (p. ej. vino vacía o el descifrado dio basura); no es un fallo del programa.
            Registro.advertencia("La respuesta en claro no tiene header/_header con Codigo/Mensaje", e);
        }
        return new Resultado(null, null);
    }

    /**
     * Devuelve header.Codigo (o _header.Codigo), o null si el JSON no tiene esa forma.
     */
    public static Integer extraerCodigo(String jsonRespuesta) {
        return parsear(jsonRespuesta).codigo;
    }
}
