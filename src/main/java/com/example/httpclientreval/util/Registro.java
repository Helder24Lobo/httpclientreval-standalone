package com.example.httpclientreval.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Logger mínimo a archivo, sin dependencias nuevas (el proyecto no traía ningún logger): los
 * {@code catch (Exception)} que solo dejaban seguir la app sin dejar rastro hacían imposible saber,
 * después de que algo fallara, qué había pasado realmente. Escribe una línea por evento en
 * {@code httpclientreval.log}, junto a profiles.json (directorio de trabajo de la app).
 *
 * No usa un framework de logging (SLF4J, Logback) a propósito: para una app de escritorio de un solo
 * usuario, un archivo de texto plano de solo-agregar es toda la infraestructura que hace falta.
 */
public final class Registro {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final Path ARCHIVO = Path.of("httpclientreval.log");

    private Registro() {
    }

    /** Un fallo real (el usuario ya lo ve en pantalla, pero sin esto el detalle se pierde al cerrar el diálogo). */
    public static void error(String contexto, Throwable causa) {
        escribir("ERROR", contexto, causa);
    }

    /** Algo que se descartó en silencio a propósito (p. ej. "no era JSON, se muestra como texto"), pero vale dejar rastro. */
    public static void advertencia(String contexto, Throwable causa) {
        escribir("WARN", contexto, causa);
    }

    private static synchronized void escribir(String nivel, String contexto, Throwable causa) {
        String encabezado = FORMATO.format(LocalDateTime.now()) + " [" + nivel + "] " + contexto
                + (causa != null ? ": " + causa : "");
        try {
            Files.writeString(ARCHIVO, encabezado + System.lineSeparator(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            if (causa != null) {
                StringWriter traza = new StringWriter();
                causa.printStackTrace(new PrintWriter(traza));
                Files.writeString(ARCHIVO, traza + System.lineSeparator(), StandardOpenOption.APPEND);
            }
        } catch (IOException noSePudoEscribir) {
            // No hay un segundo lugar donde reportar que el logger falló sin caer en el mismo problema;
            // se deja constancia en stderr nada más, por si hay una consola a la vista (IDE, terminal).
            System.err.println("No se pudo escribir en " + ARCHIVO.toAbsolutePath() + ": " + noSePudoEscribir);
        }
    }
}
