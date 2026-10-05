package com.example.httpclientreval.util;

import java.nio.file.Path;
import java.util.Locale;
import java.util.function.Function;

/**
 * Dónde guarda la app sus archivos (profiles.json y el log): una carpeta propia del usuario, fuera
 * del proyecto y del directorio desde donde se lance, para que los perfiles (con llaves y
 * credenciales) no dependan de la carpeta de trabajo ni terminen junto al código.
 *
 * Windows: %APPDATA%\httpclientreval · macOS: ~/Library/Application Support/httpclientreval ·
 * Linux: $XDG_CONFIG_HOME/httpclientreval (o ~/.config/httpclientreval). La propiedad del sistema
 * {@value #PROPIEDAD_HOME} reemplaza todo lo anterior (útil para pruebas o una instalación portátil).
 */
public final class RutasApp {

    public static final String PROPIEDAD_HOME = "httpclientreval.home";
    public static final String NOMBRE_PERFILES = "profiles.json";
    public static final String NOMBRE_LOG = "httpclientreval.log";

    private static final String CARPETA = "httpclientreval";

    private RutasApp() {
    }

    public static Path directorioDatos() {
        return directorioDatos(System.getProperty("os.name", ""), System::getenv,
                System.getProperty("user.home", ""), System.getProperty(PROPIEDAD_HOME));
    }

    public static Path archivoPerfiles() {
        return directorioDatos().resolve(NOMBRE_PERFILES);
    }

    public static Path archivoLog() {
        return directorioDatos().resolve(NOMBRE_LOG);
    }

    /** Versión pura (sin leer el entorno real) para poder probar cada sistema operativo. */
    static Path directorioDatos(String sistema, Function<String, String> entorno, String home, String sobrescrito) {
        if (sobrescrito != null && !sobrescrito.isBlank()) {
            return Path.of(sobrescrito);
        }
        String so = sistema.toLowerCase(Locale.ROOT);
        if (so.contains("win")) {
            String appData = entorno.apply("APPDATA");
            return appData != null && !appData.isBlank()
                    ? Path.of(appData, CARPETA)
                    : Path.of(home, "AppData", "Roaming", CARPETA);
        }
        if (so.contains("mac")) {
            return Path.of(home, "Library", "Application Support", CARPETA);
        }
        String xdg = entorno.apply("XDG_CONFIG_HOME");
        return xdg != null && !xdg.isBlank() ? Path.of(xdg, CARPETA) : Path.of(home, ".config", CARPETA);
    }
}
