package com.example.httpclientreval.ui;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** Formatea tamaños en bytes a algo legible (B, KB, MB), para mostrar junto al tiempo de una respuesta. */
final class FormatoTamano {

    private FormatoTamano() {
    }

    /** Bytes que ocupa el texto codificado en UTF-8 (no su longitud en caracteres: los acentos ocupan 2 bytes). */
    static long bytesUtf8(String texto) {
        return texto == null ? 0 : texto.getBytes(StandardCharsets.UTF_8).length;
    }

    static String humano(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        double kb = bytes / 1024.0;
        if (kb < 1024) {
            return formatear(kb) + " KB";
        }
        return formatear(kb / 1024.0) + " MB";
    }

    /** Un decimal, salvo que sea un número redondo (10 KB en vez de 10.0 KB). */
    private static String formatear(double valor) {
        long redondeado = Math.round(valor * 10);
        // Locale.ROOT: el punto decimal no debe depender del idioma del sistema.
        return redondeado % 10 == 0 ? String.valueOf(redondeado / 10) : String.format(Locale.ROOT, "%.1f", valor);
    }
}
