package com.example.httpclientreval.util;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * Dónde guarda la app sus archivos (profiles.json y el log): una carpeta propia del usuario, fuera
 * del proyecto y del directorio desde donde se lance, para que los perfiles (con llaves y
 * credenciales) no dependan de la carpeta de trabajo ni terminen junto al código.
 *
 * Windows: ~\.httpclientreval · macOS: ~/Library/Application Support/httpclientreval ·
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

    /**
     * Ubicaciones donde pudo quedar un profiles.json en versiones anteriores, para migrarlo en silencio
     * al arrancar: la carpeta de datos que se usaba antes (en Windows, %APPDATA%) y la carpeta de trabajo.
     */
    public static List<Path> perfilesAnteriores() {
        Path actual = archivoPerfiles().toAbsolutePath();
        List<Path> candidatos = new ArrayList<>();
        candidatos.add(directorioClasico(System.getProperty("os.name", ""), System::getenv,
                System.getProperty("user.home", "")).resolve(NOMBRE_PERFILES));
        candidatos.add(Path.of(NOMBRE_PERFILES));
        List<Path> resultado = new ArrayList<>();
        for (Path candidato : candidatos) {
            Path absoluto = candidato.toAbsolutePath();
            if (!absoluto.equals(actual) && !resultado.contains(absoluto)) {
                resultado.add(absoluto);
            }
        }
        return resultado;
    }

    /**
     * Versión pura (sin leer el entorno real) para poder probar cada sistema operativo. En Windows va
     * en el perfil del usuario y no en %APPDATA%: si la app se lanza desde una aplicación empaquetada
     * (MSIX, como el escritorio de Claude), Windows redirige %APPDATA% a una copia privada y los
     * perfiles guardados ahí no aparecen al lanzar desde otro lado.
     */
    static Path directorioDatos(String sistema, Function<String, String> entorno, String home, String sobrescrito) {
        if (sobrescrito != null && !sobrescrito.isBlank()) {
            return Path.of(sobrescrito);
        }
        if (sistema.toLowerCase(Locale.ROOT).contains("win")) {
            return Path.of(home, "." + CARPETA);
        }
        return directorioClasico(sistema, entorno, home);
    }

    /** La carpeta de datos que usaban las versiones anteriores (en Windows, %APPDATA%). */
    static Path directorioClasico(String sistema, Function<String, String> entorno, String home) {
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
