package com.example.httpclientreval.ui;

import com.example.httpclientreval.model.EnvioRegistrado;

import java.text.Normalizer;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Qué envíos del historial se muestran según el texto de búsqueda y el resultado elegido. La búsqueda
 * ignora mayúsculas y tildes, y cada palabra escrita debe aparecer en algún lado del envío (en
 * cualquier orden), así "recaudos 500" encuentra "Recaudos - Consulta" que terminó en "HTTP 500".
 */
final class FiltroEnvios {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    /** Opciones del combo de resultado; {@code resultado == null} significa sin filtrar. */
    enum Estado {
        TODOS("Todos los resultados", null),
        EXITO("Éxito", EnvioRegistrado.Resultado.EXITO),
        ERROR_NEGOCIO("Error de negocio", EnvioRegistrado.Resultado.ERROR_NEGOCIO),
        ERROR_HTTP("Error HTTP", EnvioRegistrado.Resultado.ERROR_HTTP),
        SIN_RESPUESTA("Sin respuesta", EnvioRegistrado.Resultado.SIN_RESPUESTA),
        INCOMPLETO("Incompleto", EnvioRegistrado.Resultado.INCOMPLETO);

        final String etiqueta;
        final EnvioRegistrado.Resultado resultado;

        Estado(String etiqueta, EnvioRegistrado.Resultado resultado) {
            this.etiqueta = etiqueta;
            this.resultado = resultado;
        }

        @Override
        public String toString() {
            return etiqueta;
        }
    }

    private FiltroEnvios() {
    }

    static boolean coincide(EnvioRegistrado envio, String consulta, Estado estado) {
        if (estado.resultado != null && envio.resultado() != estado.resultado) {
            return false;
        }
        return coincide(textoBuscable(envio), consulta);
    }

    /** Todo lo que se puede buscar de un envío, en una sola línea. */
    static String textoBuscable(EnvioRegistrado e) {
        StringBuilder sb = new StringBuilder();
        sb.append(HORA.format(e.hora)).append(' ')
                .append(e.perfilNombre).append(' ')
                .append(e.entornoNombre).append(' ')
                .append(e.resumen()).append(' ')
                .append(e.statusCode == null ? "" : e.statusCode).append(' ')
                .append(e.tiempoMs).append(" ms ");
        agregar(sb, e.mensajeNegocio);
        agregar(sb, e.errorEnvio);
        agregar(sb, e.resultadoPlano);
        return sb.toString();
    }

    private static void agregar(StringBuilder sb, String texto) {
        if (texto != null) {
            sb.append(' ').append(texto);
        }
    }

    /** {@code true} si cada palabra de {@code consulta} está en {@code texto}; una consulta vacía coincide con todo. */
    static boolean coincide(String texto, String consulta) {
        if (consulta == null || consulta.isBlank()) {
            return true;
        }
        String pajar = normalizar(texto);
        for (String palabra : normalizar(consulta).trim().split("\\s+")) {
            if (!pajar.contains(palabra)) {
                return false;
            }
        }
        return true;
    }

    private static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT);
    }
}
