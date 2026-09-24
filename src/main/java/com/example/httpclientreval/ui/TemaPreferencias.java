package com.example.httpclientreval.ui;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;

/**
 * Guarda el tema elegido (Claro/Oscuro/Seguir el sistema) entre corridas de
 * la app y sabe instalar el look and feel correspondiente.
 */
public final class TemaPreferencias {

    public static final String SISTEMA = "Seguir el sistema";
    public static final String CLARO = "Claro";
    public static final String OSCURO = "Oscuro";

    private static final Preferences PREFS = Preferences.userNodeForPackage(TemaPreferencias.class);
    private static final String CLAVE_TEMA = "tema";

    private TemaPreferencias() {
    }

    public static String obtenerTema() {
        return PREFS.get(CLAVE_TEMA, OSCURO);
    }

    public static void guardarTema(String tema) {
        PREFS.put(CLAVE_TEMA, tema);
    }

    /** Instala el look and feel del tema guardado. Llamar antes de crear la UI. */
    public static void instalar() throws UnsupportedLookAndFeelException {
        instalar(esOscuro(obtenerTema()));
    }

    /** Instala el look and feel claro u oscuro; para refrescar ventanas abiertas usar {@link FlatLaf#updateUI()}. */
    public static void instalar(boolean oscuro) throws UnsupportedLookAndFeelException {
        UIManager.setLookAndFeel(oscuro ? new FlatDarculaLaf() : new FlatLightLaf());
    }

    /** Resuelve si el tema indicado debe verse oscuro (para "Seguir el sistema" consulta el SO). */
    public static boolean esOscuro(String tema) {
        if (SISTEMA.equals(tema)) {
            return sistemaEnOscuro();
        }
        return !CLARO.equals(tema);
    }

    /**
     * Consulta si el sistema operativo está en modo oscuro. Java no expone
     * esto, así que se pregunta al SO con su propia herramienta. Si no se
     * puede determinar, se asume claro.
     */
    public static boolean sistemaEnOscuro() {
        String so = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        try {
            if (so.contains("win")) {
                String salida = ejecutar("reg", "query",
                        "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
                        "/v", "AppsUseLightTheme");
                return salida.matches("(?s).*AppsUseLightTheme\\s+REG_DWORD\\s+0x0\\b.*");
            }
            if (so.contains("mac")) {
                // Sin modo oscuro la clave no existe y el comando falla.
                return ejecutar("defaults", "read", "-g", "AppleInterfaceStyle").trim().equalsIgnoreCase("dark");
            }
            if (ejecutar("gsettings", "get", "org.gnome.desktop.interface", "color-scheme").contains("dark")) {
                return true;
            }
            return ejecutar("gsettings", "get", "org.gnome.desktop.interface", "gtk-theme")
                    .toLowerCase(Locale.ROOT).contains("dark");
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return false;
        }
    }

    private static String ejecutar(String... comando) throws IOException, InterruptedException {
        Process proceso = new ProcessBuilder(comando).redirectErrorStream(true).start();
        // La salida es de una línea, cabe en el búfer del pipe: se puede esperar antes de leer.
        if (!proceso.waitFor(2, TimeUnit.SECONDS)) {
            proceso.destroyForcibly();
            return "";
        }
        try (InputStream salida = proceso.getInputStream()) {
            return new String(salida.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
